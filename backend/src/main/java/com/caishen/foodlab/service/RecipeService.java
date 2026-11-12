package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.NameNormalizer;
import com.caishen.foodlab.dto.RecipeGenerateDTO;
import com.caishen.foodlab.dto.RecipeQueryDTO;
import com.caishen.foodlab.entity.AiGenerationRecord;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.IngredientSubstitute;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeFavorite;
import com.caishen.foodlab.entity.RecipeHistory;
import com.caishen.foodlab.entity.RecipeIngredient;
import com.caishen.foodlab.entity.RecipeStep;
import com.caishen.foodlab.entity.ShoppingList;
import com.caishen.foodlab.entity.ShoppingListItem;
import com.caishen.foodlab.entity.UserAvoidItem;
import com.caishen.foodlab.entity.UserIngredient;
import com.caishen.foodlab.mapper.AiGenerationRecordMapper;
import com.caishen.foodlab.mapper.IngredientMapper;
import com.caishen.foodlab.mapper.IngredientSubstituteMapper;
import com.caishen.foodlab.mapper.RecipeFavoriteMapper;
import com.caishen.foodlab.mapper.RecipeHistoryMapper;
import com.caishen.foodlab.mapper.RecipeIngredientMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.RecipeStepMapper;
import com.caishen.foodlab.mapper.UserAvoidItemMapper;
import com.caishen.foodlab.mapper.UserIngredientMapper;
import com.caishen.foodlab.vo.RecipeCardVO;
import com.caishen.foodlab.vo.RecipeDetailVO;
import com.caishen.foodlab.vo.RecipeIngredientVO;
import com.caishen.foodlab.vo.RecipeListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜谱：首页生成、推荐列表（筛选 + 排序 + 分页）、今日推荐、详情、相似、收藏、加入购物清单
 */
@Service
@RequiredArgsConstructor
public class RecipeService {

    public static final String STATUS_ONLINE = "已上线";
    public static final String SOURCE_AI = "AI生成";

    /** 难度排序权重 */
    private static final Map<String, Integer> DIFFICULTY_WEIGHT = Map.of("简单", 1, "中等", 2, "较难", 3);

    /**
     * 管理端过敏源标签 -> 常见食材关键词。
     * 标签名（蛋类 / 乳制品）本身不是食材名，必须展开成具体食材关键词才能真正过滤菜谱。
     */
    private static final Map<String, List<String>> ALLERGEN_KEYWORDS = Map.of(
            "蛋类", List.of("蛋"),
            "乳制品", List.of("牛奶", "奶油", "黄油", "芝士", "奶酪", "酸奶", "炼乳", "奶粉"),
            "麸质", List.of("面粉", "小麦", "面包", "面条", "馒头", "饺子皮", "面筋", "意面"),
            "坚果", List.of("花生", "核桃", "杏仁", "腰果", "榛子", "开心果", "松子"),
            "海鲜", List.of("虾", "蟹", "贝", "鱼", "蛤", "鱿鱼", "章鱼", "海参", "生蚝", "蚝油"),
            "花生", List.of("花生")
    );

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final IngredientSubstituteMapper substituteMapper;
    private final RecipeFavoriteMapper favoriteMapper;
    private final RecipeHistoryMapper historyMapper;
    private final UserIngredientMapper userIngredientMapper;
    private final UserAvoidItemMapper avoidItemMapper;
    private final IngredientMapper ingredientMapper;
    private final AiGenerationRecordMapper aiGenerationRecordMapper;
    private final FreeQuotaService freeQuotaService;
    private final ShoppingListService shoppingListService;
    private final AiRecommendService aiRecommendService;

    /**
     * AI 兜底创作服务。用字段注入 + @Lazy 打断循环依赖：
     * AiRecipeService → RecipeService（构建卡片）是构造器依赖，反向再走构造器会启动失败。
     */
    @Autowired
    @Lazy
    private AiRecipeService aiRecipeService;

    /** 送模型的忌口清单：用户偏好里存的 + 本次请求临时勾选的 */
    private List<String> avoidNamesForPrompt(Long userId, List<String> extra) {
        Set<String> names = new LinkedHashSet<>();
        if (userId != null) {
            avoidItemMapper.selectList(new LambdaQueryWrapper<UserAvoidItem>()
                            .eq(UserAvoidItem::getUserId, userId))
                    .forEach(item -> {
                        if (item.getName() != null && !item.getName().isBlank()) {
                            names.add(item.getName().trim());
                        }
                    });
        }
        if (extra != null) {
            extra.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).forEach(names::add);
        }
        return new ArrayList<>(names);
    }

    // ==================== 首页「生成菜谱」 ====================

    /** 按食材 + 偏好 + 忌口过滤，并记录一次生成次数（优先免费额度，用尽后消耗单次余额） */
    @Transactional(rollbackFor = Exception.class)
    public List<RecipeCardVO> generate(Long userId, RecipeGenerateDTO dto) {
        boolean useSingle = freeQuotaService.consume(userId);

        Set<String> input = normalize(dto.getIngredients());
        AvoidMatcher avoid = buildAvoidMatcher(userId, dto.getAvoidItems());

        List<Recipe> all = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, STATUS_ONLINE)
                .orderByAsc(Recipe::getId));
        Map<Long, List<RecipeIngredient>> ingMap = loadIngredients(recipeIds(all));

        List<Recipe> candidates = all.stream().filter(recipe -> {
            List<RecipeIngredient> ingredients = ingMap.getOrDefault(recipe.getId(), List.of());
            if (ingredients.stream().anyMatch(ri -> avoid.hit(ri.getName()))) {
                return false;
            }
            if (input.isEmpty()) {
                return true;
            }
            return ingredients.stream().anyMatch(ri -> input.contains(NameNormalizer.canonical(ri.getName())));
        }).collect(Collectors.toList());

        // 匹配度以「本次输入的食材 ∪ 食材篮」为准：
        // 用户刚输入的食材即便还没落库，也应当算作已有，否则卡片会显示"缺：番茄、鸡蛋"（明明就是他输入的）
        Set<String> owned = new LinkedHashSet<>(input);
        owned.addAll(ownedNames(userId));

        List<RecipeCardVO> cards = buildCards(userId, candidates, owned)
                .stream()
                .sorted(Comparator.comparing(RecipeCardVO::getMatchRate).reversed())
                .collect(Collectors.toList());

        // AI 智能推荐：大模型按食材契合度重排并生成推荐理由；
        // 未配置密钥或调用失败时静默回退上方规则排序，不影响基础功能
        applyAiRecommendation(dto, ingMap, cards);

        // 站内菜谱库完全没有覆盖用户食材时（例如只买到了冷门食材），
        // 由大模型按现有食材现场创作，避免页面只显示"0 道菜谱"。
        // 用 @Lazy 注入是为了打断 RecipeService ↔ AiRecipeService 的构造器循环依赖。
        if (cards.isEmpty()) {
            cards = new ArrayList<>(aiRecipeService.createDraftsFor(userId, dto.getIngredients(),
                    dto.getTastes(), dto.getDietType(), avoidNamesForPrompt(userId, dto.getAvoidItems()), 2));
        }

        cards = cards.stream().limit(12).collect(Collectors.toList());

        AiGenerationRecord record = new AiGenerationRecord();
        record.setUserId(userId);
        // 免费路径记 SOURCE_HOME（计每周免费额度），单次路径记 SOURCE_HOME_SINGLE（计单次余额）
        record.setSource(useSingle ? FreeQuotaService.SOURCE_HOME_SINGLE : FreeQuotaService.SOURCE_HOME);
        record.setInputIngredients(String.join(",", input));
        if (!cards.isEmpty()) {
            record.setRecipeId(cards.get(0).getId());
        }
        aiGenerationRecordMapper.insert(record);
        return cards;
    }

    /**
     * AI 推荐增强：把候选菜谱 + 用户上下文交给大模型排序并附推荐理由。
     * 任何失败（未配置/超时/解析异常）都保持原「匹配度」排序不变。
     */
    private void applyAiRecommendation(RecipeGenerateDTO dto, Map<Long, List<RecipeIngredient>> ingMap,
                                       List<RecipeCardVO> cards) {
        if (cards.isEmpty()) {
            return;
        }
        List<AiRecommendService.Candidate> candidates = new ArrayList<>();
        for (RecipeCardVO card : cards) {
            List<String> names = ingMap.getOrDefault(card.getId(), List.of())
                    .stream().map(RecipeIngredient::getName).collect(Collectors.toList());
            candidates.add(new AiRecommendService.Candidate(card.getId(), card.getName(),
                    card.getCookTime(), card.getDifficulty(), card.getTaste(), names));
        }
        List<AiRecommendService.Recommendation> ranked = aiRecommendService.rank(dto, candidates);
        if (ranked == null || ranked.isEmpty()) {
            return;
        }
        // 重排：AI 推荐顺序在前（附理由），其余按原匹配度顺序补在后面
        Map<Long, RecipeCardVO> byId = new java.util.LinkedHashMap<>();
        for (RecipeCardVO card : cards) {
            byId.putIfAbsent(card.getId(), card);
        }
        List<RecipeCardVO> reordered = new ArrayList<>();
        for (AiRecommendService.Recommendation r : ranked) {
            RecipeCardVO card = byId.remove(r.id());
            if (card != null) {
                card.setAiReason(r.reason());
                reordered.add(card);
            }
        }
        reordered.addAll(byId.values());
        cards.clear();
        cards.addAll(reordered);
    }

    // ==================== 推荐列表 ====================

    /** 菜谱推荐列表（筛选 + 排序 + 分页） */
    public RecipeListVO list(Long userId, RecipeQueryDTO query) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, STATUS_ONLINE);
        if (isNotBlank(query.getDifficulty())) {
            wrapper.eq(Recipe::getDifficulty, query.getDifficulty());
        }
        if (query.getMaxCookTime() != null) {
            wrapper.le(Recipe::getCookTime, query.getMaxCookTime());
        }
        List<String> tastes = new ArrayList<>(normalize(query.getTastes()));
        if (!tastes.isEmpty()) {
            wrapper.and(w -> {
                for (int i = 0; i < tastes.size(); i++) {
                    if (i == 0) {
                        w.like(Recipe::getTaste, tastes.get(i));
                    } else {
                        w.or().like(Recipe::getTaste, tastes.get(i));
                    }
                }
            });
        }
        // 饮食方式近似过滤：低卡按热量、生酮按碳水（素食在内存中按是否有肉类食材过滤）
        if ("低卡".equals(query.getDietType())) {
            wrapper.le(Recipe::getCalories, 200);
        }
        if ("生酮".equals(query.getDietType())) {
            wrapper.le(Recipe::getCarbs, 10);
        }
        wrapper.orderByAsc(Recipe::getId);
        List<Recipe> all = recipeMapper.selectList(wrapper);

        Map<Long, List<RecipeIngredient>> ingMap = loadIngredients(recipeIds(all));
        Map<String, String> categories = ingredientCategories();
        AvoidMatcher avoid = buildAvoidMatcher(userId, query.getAvoidItems());
        Set<String> contextIngredients = normalize(query.getIngredients());
        boolean filterByInput = !contextIngredients.isEmpty();
        Set<String> owned = filterByInput ? contextIngredients : ownedNames(userId);

        List<Recipe> kept = new ArrayList<>();
        Set<String> hiddenBy = new LinkedHashSet<>();
        int hidden = 0;
        for (Recipe recipe : all) {
            List<RecipeIngredient> ingredients = ingMap.getOrDefault(recipe.getId(), List.of());
            Set<String> hitAvoid = ingredients.stream().map(RecipeIngredient::getName)
                    .filter(avoid::hit).collect(Collectors.toSet());
            if (!hitAvoid.isEmpty()) {
                hidden++;
                hiddenBy.addAll(hitAvoid);
                continue;
            }
            if ("素食".equals(query.getDietType())
                    && ingredients.stream().anyMatch(ri -> "肉类".equals(categories.get(ri.getName())))) {
                continue;
            }
            if (filterByInput
                    && ingredients.stream().noneMatch(ri -> contextIngredients.contains(NameNormalizer.canonical(ri.getName())))) {
                continue;
            }
            // 登录用户食材篮非空时，同样隐藏与食材篮零交集（匹配度 0）的菜，
            // 避免列表里出现大量 0% 的无关推荐；游客或空食材篮不做此过滤。
            if (!filterByInput && !owned.isEmpty()
                    && ingredients.stream().noneMatch(ri -> owned.contains(NameNormalizer.canonical(ri.getName())))) {
                continue;
            }
            kept.add(recipe);
        }

        List<RecipeCardVO> cards = buildCards(userId, kept, owned);
        sortCards(cards, query.getSort());

        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long size = query.getSize() == null || query.getSize() < 1 ? 12 : query.getSize();
        int from = (int) Math.min((page - 1) * size, cards.size());
        int to = (int) Math.min(from + size, cards.size());

        RecipeListVO vo = new RecipeListVO();
        vo.setTotal((long) cards.size());
        vo.setPage(page);
        vo.setSize(size);
        vo.setPages((long) Math.ceil((double) cards.size() / size));
        vo.setList(new ArrayList<>(cards.subList(from, to)));
        vo.setHiddenCount(hidden);
        vo.setHiddenBy(new ArrayList<>(hiddenBy));
        return vo;
    }

    /** 今日推荐 */
    public List<RecipeCardVO> today(Long userId) {
        List<Recipe> recipes = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, STATUS_ONLINE)
                .orderByAsc(Recipe::getId)
                .last("LIMIT 4"));
        return buildCards(userId, recipes, ownedNames(userId));
    }

    // ==================== 详情 ====================

    public RecipeDetailVO detail(Long userId, Long id, Integer servings) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        // AI 现场创作的草稿是「待审核」状态：用户刚在推荐页看到它并点进来，
        // 这里必须放行，否则点卡片就 404；页面上会标注"AI 生成 · 待人工审核"。
        boolean aiDraft = SOURCE_AI.equals(recipe.getSource()) && "待审核".equals(recipe.getStatus());
        if (!STATUS_ONLINE.equals(recipe.getStatus()) && !aiDraft) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        if (userId != null) {
            RecipeHistory history = new RecipeHistory();
            history.setUserId(userId);
            history.setRecipeId(id);
            historyMapper.insert(history);
        }

        int targetServings = servings == null || servings < 1 ? recipe.getServings() : servings;
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, id)
                        .orderByAsc(RecipeIngredient::getSort));
        Map<Long, List<IngredientSubstitute>> substituteMap = loadSubstitutes(ingredients);
        Set<String> owned = ownedNames(userId);

        RecipeDetailVO vo = new RecipeDetailVO();
        vo.setId(recipe.getId());
        vo.setName(recipe.getName());
        vo.setCoverImage(recipe.getCoverImage());
        vo.setCuisine(recipe.getCuisine());
        vo.setDifficulty(recipe.getDifficulty());
        vo.setTaste(recipe.getTaste());
        vo.setCookTime(recipe.getCookTime());
        vo.setDescription(recipe.getDescription());
        vo.setServings(targetServings);
        vo.setSource(recipe.getSource());
        vo.setStatus(recipe.getStatus());
        vo.setFavorite(favoriteMapper.selectCount(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .eq(RecipeFavorite::getRecipeId, id)) > 0);

        RecipeDetailVO.Nutrition nutrition = new RecipeDetailVO.Nutrition();
        nutrition.setCalories(recipe.getCalories());
        nutrition.setProtein(recipe.getProtein());
        nutrition.setFat(recipe.getFat());
        nutrition.setCarbs(recipe.getCarbs());
        vo.setNutrition(nutrition);

        List<RecipeIngredientVO> ingredientVOs = new ArrayList<>();
        List<RecipeIngredientVO> missingVOs = new ArrayList<>();
        for (RecipeIngredient ri : ingredients) {
            boolean has = owned.contains(NameNormalizer.canonical(ri.getName()));
            RecipeIngredientVO itemVO = new RecipeIngredientVO();
            itemVO.setId(ri.getId());
            itemVO.setIngredientId(ri.getIngredientId());
            itemVO.setName(ri.getName());
            itemVO.setAmount(scaleAmount(ri.getAmount(), recipe.getServings(), targetServings));
            itemVO.setUnit(ri.getUnit());
            itemVO.setIsMain(ri.getIsMain() != null && ri.getIsMain() == 1);
            itemVO.setIsRequired(ri.getIsRequired() == null || ri.getIsRequired() == 1);
            itemVO.setOwned(has);
            for (IngredientSubstitute substitute : substituteMap.getOrDefault(ri.getId(), List.of())) {
                RecipeIngredientVO.Substitute sub = new RecipeIngredientVO.Substitute();
                sub.setId(substitute.getId());
                sub.setName(substitute.getName());
                sub.setRatioDesc(substitute.getRatioDesc());
                sub.setNote(substitute.getNote());
                itemVO.getSubstitutes().add(sub);
            }
            ingredientVOs.add(itemVO);
            if (!has) {
                missingVOs.add(itemVO);
            }
        }
        vo.setIngredients(ingredientVOs);
        vo.setMissingIngredients(missingVOs);
        vo.setMatchRate(matchRate(ingredientVOs, owned));

        List<RecipeStep> steps = recipeStepMapper.selectList(new LambdaQueryWrapper<RecipeStep>()
                .eq(RecipeStep::getRecipeId, id)
                .orderByAsc(RecipeStep::getStepNo));
        vo.setSteps(steps);
        vo.setSimilar(similar(recipe, userId));
        return vo;
    }

    /** 相似菜谱 */
    public List<RecipeCardVO> similar(Long recipeId, Long userId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        return similar(recipe, userId);
    }

    private List<RecipeCardVO> similar(Recipe recipe, Long userId) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, STATUS_ONLINE)
                .ne(Recipe::getId, recipe.getId())
                .and(w -> w.eq(Recipe::getCuisine, recipe.getCuisine())
                        .or().eq(Recipe::getTaste, recipe.getTaste()))
                .orderByAsc(Recipe::getId)
                .last("LIMIT 4");
        return buildCards(userId, recipeMapper.selectList(wrapper), ownedNames(userId));
    }

    // ==================== 收藏 ====================

    public void favorite(Long userId, Long recipeId) {
        requireRecipe(recipeId);
        if (favoriteMapper.selectCount(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .eq(RecipeFavorite::getRecipeId, recipeId)) > 0) {
            return;
        }
        RecipeFavorite favorite = new RecipeFavorite();
        favorite.setUserId(userId);
        favorite.setRecipeId(recipeId);
        favoriteMapper.insert(favorite);
    }

    public void unfavorite(Long userId, Long recipeId) {
        favoriteMapper.delete(new LambdaQueryWrapper<RecipeFavorite>()
                .eq(RecipeFavorite::getUserId, userId)
                .eq(RecipeFavorite::getRecipeId, recipeId));
    }

    // ==================== 加入购物清单 ====================

    /** 把菜谱中「还缺」的食材加入购物清单 */
    @Transactional(rollbackFor = Exception.class)
    public void addToShoppingList(Long userId, Long recipeId) {
        Recipe recipe = requireRecipe(recipeId);
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, recipeId)
                        .orderByAsc(RecipeIngredient::getSort));
        Set<String> owned = ownedNames(userId);
        Map<String, String> categories = ingredientCategories();

        ShoppingList list = shoppingListService.getOrCreate(userId);
        Set<String> existing = shoppingListService.detail(userId).getItems().stream()
                .map(ShoppingListItem::getName)
                .collect(Collectors.toSet());

        for (RecipeIngredient ri : ingredients) {
            if (owned.contains(NameNormalizer.canonical(ri.getName())) || existing.contains(ri.getName())) {
                continue;
            }
            shoppingListService.addItem(list.getId(), ri.getName(), ri.getAmount(), ri.getUnit(),
                    categories.getOrDefault(ri.getName(), "其他"), ri.getIngredientId(), recipe.getName());
        }
        shoppingListService.appendSourceRecipe(list, recipe.getName());
    }

    // ==================== 拍照识别 → 菜谱匹配 ====================

    /**
     * 按识别出的菜名匹配站内菜谱（AI 图片识别结果的落地环节）。
     *
     * 匹配优先级：
     * 1. 归一化后菜名完全一致（3 分）；
     * 2. 字符集合相似度 ≥ 0.65（2 分）——覆盖「西红柿炒鸡蛋」↔「番茄炒蛋」这类同菜异名；
     * 3. 菜名都对不上时，退回「识别出的食材能覆盖该菜几成主料」，覆盖 ≥ 60% 才算候选，
     *    避免出现「拍的是番茄炒蛋，却推荐莲花蒸蛋」这种明显跑偏的结果。
     *
     * @param dishes          视觉模型识别出的菜名
     * @param ingredientNames 视觉模型识别出的食材（作为「已有食材」参与匹配度计算）
     * @param limit           返回上限
     */
    public List<RecipeCardVO> matchByDishNames(Long userId, List<String> dishes,
                                               List<String> ingredientNames, int limit) {
        List<String> dishNames = dishes == null ? List.of()
                : dishes.stream().filter(d -> d != null && !d.isBlank()).map(String::trim).distinct().toList();
        Set<String> ownedInput = NameNormalizer.canonicalSet(ingredientNames);
        final Set<String> owned = ownedInput.isEmpty() ? ownedNames(userId) : ownedInput;

        List<Recipe> all = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, STATUS_ONLINE));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Integer> score = new java.util.LinkedHashMap<>();
        for (Recipe recipe : all) {
            String name = recipe.getName() == null ? "" : recipe.getName();
            for (String dish : dishNames) {
                if (NameNormalizer.canonical(name).equals(NameNormalizer.canonical(dish))) {
                    score.merge(recipe.getId(), 3, Math::max);
                } else if (NameNormalizer.similarity(name, dish) >= 0.65) {
                    score.merge(recipe.getId(), 2, Math::max);
                }
            }
        }

        // 菜名没命中：按「识别出的食材能覆盖几成主料」兜底挑候选
        if (score.isEmpty() && !owned.isEmpty()) {
            Map<Long, List<RecipeIngredient>> ingMap = loadIngredients(recipeIds(all));
            for (Recipe recipe : all) {
                List<RecipeIngredient> mains = ingMap.getOrDefault(recipe.getId(), List.of()).stream()
                        .filter(ri -> ri.getIsMain() != null && ri.getIsMain() == 1)
                        .toList();
                if (mains.isEmpty()) {
                    continue;
                }
                long hit = mains.stream()
                        .filter(ri -> owned.contains(NameNormalizer.canonical(ri.getName())))
                        .count();
                if (hit * 10 >= (long) mains.size() * 6) {
                    score.put(recipe.getId(), 1);
                }
            }
        }
        if (score.isEmpty()) {
            return new ArrayList<>();
        }

        List<Recipe> picked = all.stream()
                .filter(r -> score.containsKey(r.getId()))
                .collect(Collectors.toList());
        List<RecipeCardVO> cards = buildCards(userId, picked, owned);
        cards.sort(Comparator
                .comparingInt((RecipeCardVO c) -> score.getOrDefault(c.getId(), 0)).reversed()
                .thenComparing(Comparator.comparing(RecipeCardVO::getMatchRate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .thenComparing(RecipeCardVO::getId));
        return cards.size() > limit ? new ArrayList<>(cards.subList(0, limit)) : cards;
    }

    // ==================== 卡片构建 ====================

    public List<RecipeCardVO> buildCards(Long userId, List<Recipe> recipes) {
        return buildCards(userId, recipes, ownedNames(userId));
    }

    public List<RecipeCardVO> buildCards(Long userId, List<Recipe> recipes, Set<String> owned) {
        if (recipes == null || recipes.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = recipeIds(recipes);
        Map<Long, List<RecipeIngredient>> ingMap = loadIngredients(ids);
        Set<Long> favoriteIds = favoriteRecipeIds(userId, ids);

        List<RecipeCardVO> cards = new ArrayList<>();
        for (Recipe recipe : recipes) {
            List<RecipeIngredient> ingredients = ingMap.getOrDefault(recipe.getId(), List.of());
            RecipeCardVO card = new RecipeCardVO();
            card.setId(recipe.getId());
            card.setName(recipe.getName());
            card.setCoverImage(recipe.getCoverImage());
            card.setCuisine(recipe.getCuisine());
            card.setDifficulty(recipe.getDifficulty());
            card.setTaste(recipe.getTaste());
            card.setCookTime(recipe.getCookTime());
            card.setServings(recipe.getServings());
            card.setDescription(recipe.getDescription());
            card.setSource(recipe.getSource());
            card.setStatus(recipe.getStatus());
            card.setFavorite(favoriteIds.contains(recipe.getId()));

            List<String> missing = new ArrayList<>();
            List<String> substitutable = new ArrayList<>();
            int ownedCount = 0;
            for (RecipeIngredient ri : ingredients) {
                boolean has = owned.contains(NameNormalizer.canonical(ri.getName()));
                if (has) {
                    ownedCount++;
                } else if (ri.getIsRequired() != null && ri.getIsRequired() == 0) {
                    substitutable.add(ri.getName());
                } else {
                    missing.add(ri.getName());
                }
            }
            card.setMissing(missing);
            card.setSubstitutable(substitutable);
            card.setOwnedCount(ownedCount);
            card.setTotalCount(ingredients.size());
            card.setMatchRate(matchRate(ingredients, owned));
            cards.add(card);
        }
        return cards;
    }

    /**
     * 匹配度算法：
     * 有食材上下文时 = 主料覆盖率×70 + 必备食材覆盖率×30；
     * 无食材上下文时 = 100 - 缺必备×10 - 缺可替代×5（下限 40）。
     */
    private int matchRate(List<?> ingredients, Set<String> owned) {
        int mainTotal = 0;
        int mainOwned = 0;
        int requiredTotal = 0;
        int requiredOwned = 0;
        int missingRequired = 0;
        int missingOptional = 0;
        for (Object obj : ingredients) {
            String name;
            boolean isMain;
            boolean isRequired;
            if (obj instanceof RecipeIngredient ri) {
                name = ri.getName();
                isMain = ri.getIsMain() != null && ri.getIsMain() == 1;
                isRequired = ri.getIsRequired() == null || ri.getIsRequired() == 1;
            } else {
                RecipeIngredientVO vo = (RecipeIngredientVO) obj;
                name = vo.getName();
                isMain = Boolean.TRUE.equals(vo.getIsMain());
                isRequired = Boolean.TRUE.equals(vo.getIsRequired());
            }
            boolean has = owned.contains(name) || owned.contains(NameNormalizer.canonical(name));
            if (isMain) {
                mainTotal++;
                if (has) {
                    mainOwned++;
                }
            }
            if (isRequired) {
                requiredTotal++;
                if (has) {
                    requiredOwned++;
                }
            }
            if (!has) {
                if (isRequired) {
                    missingRequired++;
                } else {
                    missingOptional++;
                }
            }
        }
        if (owned.isEmpty()) {
            return Math.max(40, 100 - missingRequired * 10 - missingOptional * 5);
        }
        double mainRatio = mainTotal == 0 ? 1d : (double) mainOwned / mainTotal;
        double requiredRatio = requiredTotal == 0 ? 1d : (double) requiredOwned / requiredTotal;
        return (int) Math.round(mainRatio * 70 + requiredRatio * 30);
    }

    private void sortCards(List<RecipeCardVO> cards, String sort) {
        String type = sort == null ? "匹配度" : sort;
        switch (type) {
            case "耗时" -> cards.sort(Comparator.comparing(RecipeCardVO::getCookTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            case "难度" -> cards.sort(Comparator.comparing(
                    c -> DIFFICULTY_WEIGHT.getOrDefault(c.getDifficulty(), 9)));
            default -> cards.sort(Comparator.comparing(RecipeCardVO::getMatchRate).reversed()
                    .thenComparing(RecipeCardVO::getId));
        }
    }

    // ==================== 公共辅助 ====================

    /** 用户食材篮名称集合 */
    public Set<String> ownedNames(Long userId) {
        if (userId == null) {
            return new LinkedHashSet<>();
        }
        // 归一化：食材篮里存的是「西红柿」，菜谱写的是「番茄」，不归一化会永远匹配不上
        return NameNormalizer.canonicalSet(userIngredientMapper.selectList(
                        new LambdaQueryWrapper<UserIngredient>().eq(UserIngredient::getUserId, userId))
                .stream().map(UserIngredient::getName).toList());
    }

    /** 忌口 + 过敏源 + 入参忌口 -> 匹配器（过敏源标签展开为食材关键词） */
    private AvoidMatcher buildAvoidMatcher(Long userId, List<String> extra) {
        Set<String> exact = new LinkedHashSet<>();
        Set<String> keywords = new LinkedHashSet<>();
        if (userId != null) {
            List<UserAvoidItem> items = avoidItemMapper.selectList(new LambdaQueryWrapper<UserAvoidItem>()
                    .eq(UserAvoidItem::getUserId, userId));
            for (UserAvoidItem item : items) {
                // type=2 为过敏源标签，需要展开关键词；type=1 为具体忌口食材，按全名匹配
                addAvoidTerm(item.getName(), item.getType() != null && item.getType() == 2, exact, keywords);
            }
        }
        if (extra != null) {
            for (String name : extra) {
                if (name == null || name.isBlank()) {
                    continue;
                }
                // 前端传的是「忌口 + 过敏源」合并列表，命中已知过敏源标签时同样展开
                addAvoidTerm(name, ALLERGEN_KEYWORDS.containsKey(name.trim()), exact, keywords);
            }
        }
        return new AvoidMatcher(exact, keywords);
    }

    private void addAvoidTerm(String name, boolean allergen, Set<String> exact, Set<String> keywords) {
        if (name == null || name.isBlank()) {
            return;
        }
        String term = name.trim();
        exact.add(term);
        if (allergen) {
            keywords.add(term);
            keywords.addAll(ALLERGEN_KEYWORDS.getOrDefault(term, List.of()));
        }
    }

    /** 忌口匹配：具体食材全名匹配 + 过敏源标签关键词包含匹配（如「蛋类」命中「鸡蛋」） */
    public static class AvoidMatcher {
        private final Set<String> exact;
        private final Set<String> keywords;

        AvoidMatcher(Set<String> exact, Set<String> keywords) {
            this.exact = exact;
            this.keywords = keywords;
        }

        public boolean hit(String ingredientName) {
            if (ingredientName == null || ingredientName.isBlank()) {
                return false;
            }
            if (exact.contains(ingredientName)) {
                return true;
            }
            for (String keyword : keywords) {
                if (!keyword.isBlank() && ingredientName.contains(keyword)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 食材名 -> 分类 */
    public Map<String, String> ingredientCategories() {
        return ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>())
                .stream().collect(Collectors.toMap(Ingredient::getName, Ingredient::getCategory, (a, b) -> a));
    }

    private Map<Long, List<RecipeIngredient>> loadIngredients(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) {
            return Map.of();
        }
        return recipeIngredientMapper.selectList(new LambdaQueryWrapper<RecipeIngredient>()
                        .in(RecipeIngredient::getRecipeId, recipeIds)
                        .orderByAsc(RecipeIngredient::getSort))
                .stream().collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
    }

    private Map<Long, List<IngredientSubstitute>> loadSubstitutes(List<RecipeIngredient> ingredients) {
        List<Long> ids = ingredients.stream().map(RecipeIngredient::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return substituteMapper.selectList(new LambdaQueryWrapper<IngredientSubstitute>()
                        .in(IngredientSubstitute::getRecipeIngredientId, ids))
                .stream().collect(Collectors.groupingBy(IngredientSubstitute::getRecipeIngredientId));
    }

    private Set<Long> favoriteRecipeIds(Long userId, List<Long> recipeIds) {
        if (userId == null || recipeIds.isEmpty()) {
            return Set.of();
        }
        return favoriteMapper.selectList(new LambdaQueryWrapper<RecipeFavorite>()
                        .eq(RecipeFavorite::getUserId, userId)
                        .in(RecipeFavorite::getRecipeId, recipeIds))
                .stream().map(RecipeFavorite::getRecipeId).collect(Collectors.toSet());
    }

    private Recipe requireRecipe(Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new BusinessException("菜谱不存在");
        }
        return recipe;
    }

    private List<Long> recipeIds(List<Recipe> recipes) {
        return recipes.stream().map(Recipe::getId).collect(Collectors.toList());
    }

    private Set<String> normalize(List<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        // 统一归一化为库内书面名（西红柿→番茄），否则输入侧与菜谱侧永远对不上
        return values.stream().filter(s -> s != null && !s.isBlank())
                .map(NameNormalizer::canonical)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 按份量缩放数量（仅数值型） */
    private String scaleAmount(String amount, Integer fromServings, Integer toServings) {
        if (amount == null || fromServings == null || toServings == null
                || fromServings.equals(toServings) || fromServings <= 0) {
            return amount;
        }
        try {
            BigDecimal value = new BigDecimal(amount.trim());
            BigDecimal scaled = value.multiply(BigDecimal.valueOf(toServings))
                    .divide(BigDecimal.valueOf(fromServings), 1, java.math.RoundingMode.HALF_UP);
            return scaled.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
