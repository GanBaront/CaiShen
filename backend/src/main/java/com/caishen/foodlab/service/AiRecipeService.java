package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.NameNormalizer;
import com.caishen.foodlab.config.AppProperties;
import com.caishen.foodlab.dto.AiRecipeGenerateDTO;
import com.caishen.foodlab.dto.AiRecipeResultDTO;
import com.caishen.foodlab.entity.AiGenerationRecord;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.IngredientSubstitute;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeIngredient;
import com.caishen.foodlab.entity.RecipeStep;
import com.caishen.foodlab.mapper.AiGenerationRecordMapper;
import com.caishen.foodlab.mapper.IngredientMapper;
import com.caishen.foodlab.mapper.IngredientSubstituteMapper;
import com.caishen.foodlab.mapper.RecipeIngredientMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.RecipeStepMapper;
import com.caishen.foodlab.vo.AiRecipePreviewVO;
import com.caishen.foodlab.vo.RecipeCardVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AI 新菜谱：真实调用 OpenAI 兼容的大模型接口，要求返回严格 JSON 后落库为菜谱。
 * 未配置 API Key 时直接返回明确错误，不伪造/编造菜谱数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecipeService {

    /** 要求的 JSON 结构说明 */
    private static final String JSON_SCHEMA = """
            严格返回一个 JSON 对象（不要包含解释文字，不要使用 markdown 代码块），结构如下：
            {
              "name": "菜名",
              "cuisine": "家常菜|创意菜|轻食",
              "difficulty": "简单|中等|较难",
              "taste": "口味，如 清淡/酸甜/咸香",
              "cookTime": 整数分钟,
              "description": "一句话简介",
              "servings": 整数人份,
              "ingredients": [
                {"name": "食材名", "amount": "数量(如 2 / 少许)", "unit": "单位", "isMain": true, "isRequired": true,
                 "substitutes": [{"name": "替代品", "ratioDesc": "替代比例", "note": "口味衔接说明"}]}
              ],
              "steps": [
                {"stepNo": 1, "title": "步骤标题", "description": "图文说明", "tips": "关键提示"}
              ],
              "nutrition": {"calories": 每份热量kcal, "protein": 蛋白质g, "fat": 脂肪g, "carbs": 碳水g}
            }
            """;

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final IngredientSubstituteMapper substituteMapper;
    private final IngredientMapper ingredientMapper;
    private final AiGenerationRecordMapper aiGenerationRecordMapper;
    private final FreeQuotaService freeQuotaService;
    private final RecipeService recipeService;
    private final AppProperties appProperties;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /** AI 新菜谱预览（取库中一条 AI 生成菜谱作为示例） */
    public AiRecipePreviewVO preview(Long userId) {
        Recipe recipe = recipeMapper.selectOne(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getSource, RecipeService.SOURCE_AI)
                .orderByDesc(Recipe::getId)
                .last("LIMIT 1"));
        AiRecipePreviewVO vo = new AiRecipePreviewVO();
        boolean member = freeQuotaService.isMember(userId);
        vo.setNeedPurchase(!member);
        vo.setTip(member ? null : "会员 ¥" + appProperties.getMemberMonthPrice()
                + "/月，或单次 ¥" + appProperties.getAiSinglePrice() + " 体验 AI 新菜谱");
        if (recipe == null) {
            return vo;
        }
        vo.setId(recipe.getId());
        vo.setName(recipe.getName());
        vo.setCookTime(recipe.getCookTime());
        vo.setDifficulty(recipe.getDifficulty());
        vo.setTaste(recipe.getTaste());
        vo.setDescription(recipe.getDescription());
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipe.getId())
                        .orderByAsc(RecipeIngredient::getSort));
        List<String> names = new ArrayList<>();
        for (RecipeIngredient ri : ingredients) {
            names.add(ri.getName());
        }
        vo.setIngredients(names);
        return vo;
    }

    /** 会员 / 单次「AI 新菜谱」生成 */
    @Transactional(rollbackFor = Exception.class)
    public RecipeCardVO generate(Long userId, AiRecipeGenerateDTO dto) {
        checkEntitlement(userId);

        List<String> ingredients = dto.getIngredients() == null ? List.of()
                : dto.getIngredients().stream().filter(s -> s != null && !s.isBlank()).toList();
        if (ingredients.isEmpty()) {
            throw new BusinessException("请先录入食材再生成 AI 新菜谱");
        }

        AiRecipeResultDTO result = callModel(String.join("、", ingredients));
        Recipe recipe = persistDraft(result);

        AiGenerationRecord record = new AiGenerationRecord();
        record.setUserId(userId);
        record.setSource(blankTo(dto.getSource(), FreeQuotaService.SOURCE_MEMBER_AI));
        record.setInputIngredients(String.join(",", ingredients));
        record.setRecipeId(recipe.getId());
        aiGenerationRecordMapper.insert(record);

        List<RecipeCardVO> cards = recipeService.buildCards(userId, List.of(recipe));
        return cards.get(0);
    }

    // ==================== 站内无匹配时的兜底创作 ====================

    /**
     * 站内菜谱库覆盖不到用户食材时的兜底：让模型按现有食材现场创作 count 道菜。
     *
     * 触发场景：首页「生成菜谱」筛完只剩 0 道，页面只能显示"没有找到匹配的菜谱"。
     * 设计取舍：
     * - 落库状态为「待审核」，后台仍可审核/下架，不绕过内容审核流程；
     * - 但允许在详情页查看（见 RecipeService.detail），否则用户点了卡片就 404；
     * - 任何失败（未配置 / 超时 / 解析异常）都返回空列表，不影响主流程；
     * - 命中用户忌口/过敏源的草稿直接丢弃，不让模型"自己发挥"。
     *
     * @return 已落库并转成卡片的草稿（可能为空）
     */
    public List<RecipeCardVO> createDraftsFor(Long userId, List<String> ingredients, List<String> tastes,
                                              String dietType, List<String> avoidItems, int count) {
        if (!llmClient.isConfigured() || ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<AiRecipeResultDTO> drafts;
        try {
            drafts = callDraftModel(ingredients, tastes, dietType, avoidItems, count);
        } catch (Exception e) {
            log.warn("AI 兜底创作失败，保持原推荐结果: {}", e.getMessage());
            return List.of();
        }
        if (drafts.isEmpty()) {
            return List.of();
        }

        List<String> avoid = avoidItems == null ? List.of() : avoidItems;
        List<Recipe> created = new ArrayList<>();
        for (AiRecipeResultDTO draft : drafts) {
            if (draft == null || draft.getName() == null || draft.getName().isBlank()) {
                continue;
            }
            if (hitsAvoid(draft, avoid)) {
                log.info("AI 草稿「{}」命中忌口，已丢弃", draft.getName());
                continue;
            }
            created.add(persistDraft(draft));
        }
        if (created.isEmpty()) {
            return List.of();
        }
        Set<String> owned = NameNormalizer.canonicalSet(ingredients);
        List<RecipeCardVO> cards = recipeService.buildCards(userId, created, owned);
        for (RecipeCardVO card : cards) {
            card.setAiGenerated(true);
            card.setAiReason("站内暂无匹配菜谱，AI 用你现有的食材现场创作");
        }
        return cards;
    }

    /** 一次让模型产出多道草稿（单个 JSON 数组，比多次调用省时延） */
    private List<AiRecipeResultDTO> callDraftModel(List<String> ingredients, List<String> tastes,
                                                   String dietType, List<String> avoidItems, int count) {
        String systemPrompt = """
                你是「材神食验室」的中餐菜谱创作助手。请仅使用用户手边已有的食材（可另用盐、油、生抽等基础调味）
                创作 %d 道家常菜，做法必须真实可行、步骤可执行。
                严格返回一个 JSON 数组（不要解释文字，不要 markdown 代码块），每个元素结构：
                {"name":"菜名","cuisine":"家常菜","difficulty":"简单|中等|较难","taste":"口味","cookTime":整数分钟,
                 "description":"一句话简介","servings":整数人份,
                 "ingredients":[{"name":"食材名","amount":"数量","unit":"单位","isMain":true,"isRequired":true}],
                 "steps":[{"stepNo":1,"title":"步骤标题","description":"做法说明","tips":"关键提示"}],
                 "nutrition":{"calories":每份热量kcal,"protein":蛋白质g,"fat":脂肪g,"carbs":碳水g}}
                要求：每道菜 4-6 个食材、3-5 个步骤；绝不使用用户忌口或过敏的食材。
                """.formatted(Math.max(1, count));

        StringBuilder user = new StringBuilder();
        user.append("我的食材：").append(String.join("、", ingredients)).append('\n');
        user.append("口味偏好：").append(tastes == null || tastes.isEmpty() ? "不限" : String.join("、", tastes)).append('\n');
        user.append("饮食方式：").append(dietType == null || dietType.isBlank() ? "不限" : dietType).append('\n');
        user.append("忌口/过敏（绝对不能用）：").append(avoidItems == null || avoidItems.isEmpty() ? "无" : String.join("、", avoidItems)).append('\n');
        user.append("请只返回 JSON 数组。");

        String content = llmClient.chat(systemPrompt, user.toString(), 0.8);
        return parseDraftArray(content);
    }

    private List<AiRecipeResultDTO> parseDraftArray(String content) {
        try {
            return objectMapper.readValue(LlmClient.extractJsonArray(content),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiRecipeResultDTO.class));
        } catch (Exception e) {
            // 有些模型会返回 {"recipes":[...]}，再兜一次
            try {
                var node = objectMapper.readTree(LlmClient.extractJson(content));
                for (String key : new String[]{"recipes", "data", "list", "items"}) {
                    var arr = node.get(key);
                    if (arr != null && arr.isArray()) {
                        return objectMapper.convertValue(arr,
                                objectMapper.getTypeFactory().constructCollectionType(List.class, AiRecipeResultDTO.class));
                    }
                }
            } catch (Exception ignored) {
                // 落到下面的告警
            }
            log.warn("AI 草稿返回内容无法解析为菜谱数组: {}", content);
            return List.of();
        }
    }

    /** 草稿里出现忌口/过敏食材则整条丢弃（模型不一定听话，必须自己校验） */
    private boolean hitsAvoid(AiRecipeResultDTO draft, List<String> avoid) {
        if (avoid.isEmpty() || draft.getIngredients() == null) {
            return false;
        }
        for (AiRecipeResultDTO.Ingredient item : draft.getIngredients()) {
            String name = NameNormalizer.canonical(item.getName());
            if (name.isEmpty()) {
                continue;
            }
            for (String a : avoid) {
                String avoidName = NameNormalizer.canonical(a);
                if (!avoidName.isEmpty() && (name.contains(avoidName) || avoidName.contains(name))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 把模型返回的菜谱结构落库（菜谱 + 食材 + 步骤），状态固定为待审核 */
    private Recipe persistDraft(AiRecipeResultDTO result) {
        Recipe recipe = new Recipe();
        recipe.setName(blankTo(result.getName(), "AI 创意菜"));
        recipe.setCuisine(blankTo(result.getCuisine(), "创意菜"));
        recipe.setDifficulty(blankTo(result.getDifficulty(), "中等"));
        recipe.setTaste(blankTo(result.getTaste(), "清淡"));
        recipe.setCookTime(result.getCookTime() == null || result.getCookTime() <= 0 ? 30 : result.getCookTime());
        recipe.setDescription(result.getDescription());
        recipe.setServings(result.getServings() == null || result.getServings() <= 0 ? 2 : result.getServings());
        recipe.setSource(RecipeService.SOURCE_AI);
        recipe.setStatus("待审核");
        if (result.getNutrition() != null) {
            recipe.setCalories(result.getNutrition().getCalories());
            recipe.setProtein(result.getNutrition().getProtein());
            recipe.setFat(result.getNutrition().getFat());
            recipe.setCarbs(result.getNutrition().getCarbs());
        }
        recipeMapper.insert(recipe);

        if (result.getIngredients() != null) {
            int sort = 1;
            for (AiRecipeResultDTO.Ingredient item : result.getIngredients()) {
                if (item.getName() == null || item.getName().isBlank()) {
                    continue;
                }
                RecipeIngredient ri = new RecipeIngredient();
                ri.setRecipeId(recipe.getId());
                ri.setIngredientId(resolveIngredientId(item.getName()));
                ri.setName(item.getName().trim());
                ri.setAmount(item.getAmount());
                ri.setUnit(item.getUnit());
                ri.setIsMain(Boolean.FALSE.equals(item.getIsMain()) ? 0 : 1);
                ri.setIsRequired(Boolean.FALSE.equals(item.getIsRequired()) ? 0 : 1);
                ri.setSort(sort++);
                recipeIngredientMapper.insert(ri);

                if (item.getSubstitutes() != null) {
                    for (AiRecipeResultDTO.Substitute sub : item.getSubstitutes()) {
                        if (sub.getName() == null || sub.getName().isBlank()) {
                            continue;
                        }
                        IngredientSubstitute substitute = new IngredientSubstitute();
                        substitute.setRecipeIngredientId(ri.getId());
                        substitute.setIngredientId(resolveIngredientId(sub.getName()));
                        substitute.setName(sub.getName().trim());
                        substitute.setRatioDesc(sub.getRatioDesc());
                        substitute.setNote(sub.getNote());
                        substituteMapper.insert(substitute);
                    }
                }
            }
        }

        if (result.getSteps() != null) {
            int sort = 1;
            for (AiRecipeResultDTO.Step step : result.getSteps()) {
                RecipeStep entity = new RecipeStep();
                entity.setRecipeId(recipe.getId());
                entity.setStepNo(step.getStepNo() == null ? sort : step.getStepNo());
                entity.setTitle(blankTo(step.getTitle(), "步骤 " + sort));
                entity.setDescription(step.getDescription());
                entity.setTips(step.getTips());
                entity.setSort(sort++);
                recipeStepMapper.insert(entity);
            }
        }
        return recipe;
    }

    /** 会员不限次；非会员按统一的「单次余额」放行（与首页生成共用同一口径，防止单次被重复消费） */
    private void checkEntitlement(Long userId) {
        if (freeQuotaService.isMember(userId)) {
            return;
        }
        if (freeQuotaService.singleRemaining(userId) <= 0) {
            throw new BusinessException("请开通会员或购买单次体验后再生成 AI 新菜谱");
        }
    }

    /** 调用 OpenAI 兼容的大模型接口（统一走 LlmClient） */
    private AiRecipeResultDTO callModel(String ingredientText) {
        String systemPrompt = "你是一名中餐菜谱创作助手。请根据用户手边的食材，创作一道新颖但可行的菜谱。"
                + "只输出 JSON，不要输出任何解释文字或 markdown 代码块。\n" + JSON_SCHEMA;
        String userPrompt = "我手边的食材是：" + ingredientText + "。请只使用这些食材（可加盐、油等基础调味）创作菜谱，并严格按给定 JSON 结构返回。";

        String content = llmClient.chat(systemPrompt, userPrompt, 0.8);
        try {
            return objectMapper.readValue(LlmClient.extractJson(content), AiRecipeResultDTO.class);
        } catch (Exception e) {
            log.warn("大模型返回内容无法解析为菜谱 JSON: {}", content);
            throw new BusinessException("大模型返回内容无法解析为菜谱 JSON，请稍后重试");
        }
    }

    private Long resolveIngredientId(String name) {
        List<Ingredient> list = ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                .eq(Ingredient::getName, name.trim())
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
