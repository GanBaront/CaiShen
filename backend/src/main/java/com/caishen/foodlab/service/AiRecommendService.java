package com.caishen.foodlab.service;

import com.caishen.foodlab.dto.RecipeGenerateDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 菜谱推荐：把「用户食材 + 口味/饮食偏好 + 忌口过敏 + 人数」连同候选菜谱一起交给大模型，
 * 由模型挑选并排序、给出结合用户食材的推荐理由。
 *
 * 设计原则（与首页免费基础功能解耦）：
 * - 未配置模型密钥、模型调用失败、返回格式异常时，一律返回 null，由调用方回退到原规则排序，
 *   基础「生成菜谱」功能不因 AI 异常而不可用；
 * - 推荐结果只允许引用候选列表中的菜谱 ID，防止模型编造不存在的菜。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendService {

    private static final String SYSTEM_PROMPT = """
            你是「材神食验室」的菜谱推荐助手。根据用户的食材与偏好，从候选菜谱中挑选最合适的菜并排序。
            严格返回一个 JSON 对象（不要解释文字，不要 markdown 代码块），结构：
            {"recommendations":[{"id":候选菜谱ID,"reason":"一句话推荐理由，必须点明用户已有哪些食材/还缺什么，不超过40字"}]}
            推荐规则：
            1. 最多推荐 12 道，按推荐优先级从高到低排序；
            2. 优先推荐用户现有食材覆盖率高的菜谱（食材利用率优先）；
            3. 绝不推荐包含用户忌口或过敏食材的菜谱；
            4. 兼顾口味偏好、饮食方式、难度与耗时、用餐人数；
            5. 只能使用候选列表中出现的 ID，不得编造。
            """;

    /** 送入模型的候选菜谱上限（控制 token 与时延） */
    private static final int MAX_CANDIDATES = 30;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /** 候选菜谱（送模型的精简视图） */
    public record Candidate(long id, String name, Integer cookTime, String difficulty,
                            String taste, List<String> ingredients) {
    }

    /** 一条推荐：菜谱 ID + 推荐理由 */
    public record Recommendation(long id, String reason) {
    }

    /**
     * AI 排序推荐。
     * @return 有序推荐列表（id 均来自候选集）；未配置 / 失败 / 无有效结果时返回 null
     */
    public List<Recommendation> rank(RecipeGenerateDTO dto, List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (!llmClient.isConfigured()) {
            log.info("未配置大模型，AI 推荐跳过，使用规则排序");
            return null;
        }
        List<Candidate> pool = candidates.size() > MAX_CANDIDATES
                ? new ArrayList<>(candidates.subList(0, MAX_CANDIDATES)) : candidates;
        try {
            String content = llmClient.chat(SYSTEM_PROMPT, buildUserPrompt(dto, pool), 0.3);
            return parse(content, pool);
        } catch (Exception e) {
            log.warn("AI 推荐排序失败，回退规则排序: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(RecipeGenerateDTO dto, List<Candidate> pool) {
        List<String> ingredients = dto.getIngredients() == null ? List.of() : dto.getIngredients();
        List<String> tastes = dto.getTastes() == null ? List.of() : dto.getTastes();
        List<String> avoid = dto.getAvoidItems() == null ? List.of() : dto.getAvoidItems();
        int servings = dto.getServings() == null || dto.getServings() < 1 ? 2 : dto.getServings();

        StringBuilder sb = new StringBuilder();
        sb.append("用户食材：").append(ingredients.isEmpty() ? "（未填写，按大众家常场景推荐）" : String.join("、", ingredients)).append('\n');
        sb.append("口味偏好：").append(tastes.isEmpty() ? "不限" : String.join("、", tastes)).append('\n');
        sb.append("饮食方式：").append(dto.getDietType() == null || dto.getDietType().isBlank() ? "不限" : dto.getDietType()).append('\n');
        sb.append("忌口/过敏：").append(avoid.isEmpty() ? "无" : String.join("、", avoid)).append('\n');
        sb.append("用餐人数：").append(servings).append(" 人\n");
        sb.append("候选菜谱：\n");
        for (Candidate c : pool) {
            sb.append("- ID=").append(c.id())
                    .append(" ").append(c.name())
                    .append(" | 耗时").append(c.cookTime() == null ? "—" : c.cookTime() + "分钟")
                    .append(" | 难度").append(c.difficulty() == null ? "—" : c.difficulty())
                    .append(" | 口味").append(c.taste() == null ? "—" : c.taste())
                    .append(" | 食材：").append(c.ingredients().isEmpty() ? "—" : String.join("、", c.ingredients()))
                    .append('\n');
        }
        sb.append("请按推荐优先级挑选并排序，只返回 JSON。");
        return sb.toString();
    }

    private List<Recommendation> parse(String content, List<Candidate> pool) {
        try {
            JsonNode node = objectMapper.readTree(LlmClient.extractJson(content));
            JsonNode arr = node.get("recommendations");
            if (arr == null || !arr.isArray()) {
                return null;
            }
            Set<Long> validIds = new HashSet<>();
            for (Candidate c : pool) {
                validIds.add(c.id());
            }
            List<Recommendation> result = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            for (JsonNode n : arr) {
                long id = n.path("id").asLong(0);
                String reason = n.path("reason").asText("");
                if (id > 0 && validIds.contains(id) && !seen.contains(id)
                        && reason != null && !reason.isBlank()) {
                    seen.add(id);
                    result.add(new Recommendation(id, reason.trim()));
                }
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            log.warn("AI 推荐结果解析失败: {}", content);
            return null;
        }
    }
}
