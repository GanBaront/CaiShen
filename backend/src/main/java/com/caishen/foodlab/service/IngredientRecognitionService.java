package com.caishen.foodlab.service;

import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.config.AppProperties;
import com.caishen.foodlab.vo.RecognitionVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * 食材 / 菜品图片识别：调用视觉大模型（默认智谱 glm-4v-flash）。
 *
 * 关键实现：
 * 1. 上传接口返回的是站内相对 URL（如 /uploads/20260101/xx.jpg），外部模型服务无法访问，
 *    因此先解析为本地文件、转成 base64 再送入视觉模型；
 * 2. 图片入参格式按服务商自动适配（智谱只认裸 base64，OpenAI 系认 data URI），见 LlmClient；
 * 3. 识别出菜名后顺带匹配站内菜谱，让「拍照识菜」能直接跳到做法；
 * 4. 未配置 API Key 时返回明确错误，不伪造识别结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientRecognitionService {

    private static final String SYSTEM_PROMPT = """
            你是专业的中餐食材与菜品识别助手。用户给出的图片可能是：生鲜食材（散装蔬菜、肉、蛋、水果、海鲜、豆制品等）、
            冰箱/厨房场景（多食材同框）、或已经做好的菜品。
            识别要求：
            1. 若是食材或食材同框：在 ingredients 中列出所有可识别的生鲜食材名称（用通用中文名，如"番茄"而不是具体品种名），最多 12 项；
            2. 若是已成菜：在 dishes 中给出标准菜名（如"番茄炒蛋"、"宫保鸡丁"），并在 ingredients 中拆解出它的主要食材，最多 8 项；
            3. 无法食用或无法判断的内容不要输出；两个数组都没有内容时分别返回空数组。
            严格返回 JSON（不要解释文字，不要 markdown 代码块）：
            {"dishes":["菜名"],"ingredients":["食材1","食材2"]}
            """;

    private static final String USER_PROMPT = "请识别这张图片中的食材或菜品，严格按给定 JSON 结构返回。";

    /** 送入视觉模型的图片体积上限（base64 后智谱限制 5MB，这里按原始字节留足余量） */
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;

    private final AppProperties appProperties;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final RecipeService recipeService;

    /**
     * 识别图片中的食材 / 菜品，并匹配站内菜谱。
     *
     * @param userId   可为空（游客也能识别，只是匹配度不参考食材篮）
     * @param imageUrl 上传接口返回的站内 URL（/uploads/...）或公网 http(s) 图片地址
     */
    public RecognitionVO recognize(Long userId, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BusinessException("请先上传图片");
        }
        if (!llmClient.isVisionConfigured()) {
            throw new BusinessException("图片识别功能未启用：请配置视觉大模型密钥（ai.vision-api-key），"
                    + "或在后台「系统配置 → AI 服务」中填写后重试");
        }
        String imageData = resolveImageData(imageUrl.trim());

        String content = llmClient.chatWithImage(SYSTEM_PROMPT, USER_PROMPT, imageData, 0.2);
        RecognitionVO vo = parseResult(content);
        // 识别到菜名 → 找站内同款菜谱，直接给出做法
        vo.setMatchedRecipes(recipeService.matchByDishNames(userId, vo.getDishes(), vo.getIngredients(), 4));
        return vo;
    }

    // ==================== 图片数据解析 ====================

    /**
     * 把图片引用转换为视觉模型可读的数据：
     * 站内相对 URL → 读本地文件转 base64 data URI；公网 URL → 原样透传（由模型服务拉取）。
     */
    private String resolveImageData(String imageUrl) {
        String prefix = appProperties.getUploadUrlPrefix();
        if (prefix != null && imageUrl.startsWith(prefix)) {
            return toDataUri(readLocalFile(imageUrl, prefix));
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")
                || imageUrl.startsWith("data:")) {
            return imageUrl;
        }
        // 兼容只传文件名的调用
        return toDataUri(readLocalFile("/" + imageUrl.replaceFirst("^/+", ""), prefix));
    }

    /** 站内相对 URL → 本地文件（带目录穿越校验），文件不存在时抛出明确错误 */
    private Path readLocalFile(String imageUrl, String prefix) {
        String rel = imageUrl.startsWith(prefix) ? imageUrl.substring(prefix.length()) : imageUrl;
        rel = rel.replaceFirst("^/+", "");
        Path baseDir = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
        Path file = baseDir.resolve(rel).normalize();
        if (!file.startsWith(baseDir)) {
            throw new BusinessException("图片路径不合法");
        }
        if (!Files.isRegularFile(file)) {
            throw new BusinessException("图片不存在或已删除，请重新上传后再识别");
        }
        return file;
    }

    private String toDataUri(Path file) {
        try {
            long size = Files.size(file);
            if (size > MAX_IMAGE_BYTES) {
                throw new BusinessException("图片过大（超过 4MB），请压缩后再上传识别");
            }
            byte[] bytes = Files.readAllBytes(file);
            String mime = mimeOf(file.getFileName().toString());
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("读取上传图片失败：" + e.getMessage());
        }
    }

    private String mimeOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    // ==================== 结果解析 ====================

    /**
     * 解析模型返回。除了标准 {"dishes":[],"ingredients":[]}，
     * 还兼容模型偶尔给出的中文 key（菜品/食材）与纯数组形式，避免因格式差异整次识别作废。
     */
    private RecognitionVO parseResult(String content) {
        RecognitionVO vo = new RecognitionVO();
        JsonNode node;
        try {
            node = objectMapper.readTree(LlmClient.extractJson(content));
        } catch (Exception objectFailed) {
            return parseArrayFallback(content);
        }
        if (node == null || node.isNull()) {
            return parseArrayFallback(content);
        }
        if (node.isArray()) {
            fillNames(node, vo.getIngredients(), 12);
        } else {
            fillNames(firstPresent(node, "dishes", "菜品", "菜名", "dishes_list"), vo.getDishes(), 6);
            fillNames(firstPresent(node, "ingredients", "食材", "原料", "原料清单"), vo.getIngredients(), 12);
        }
        if (vo.getIngredients().isEmpty() && vo.getDishes().isEmpty()) {
            parseArrayFallback(content, vo);
        }
        if (vo.getIngredients().isEmpty() && vo.getDishes().isEmpty()) {
            throw new BusinessException("未从图片中识别出食材，请换一张更清晰的图片");
        }
        return vo;
    }

    private JsonNode firstPresent(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode child = node.get(key);
            if (child != null && child.isArray()) {
                return child;
            }
        }
        return null;
    }

    /** 模型直接返回 ["番茄","鸡蛋"] 这类纯数组时的兜底 */
    private RecognitionVO parseArrayFallback(String content) {
        RecognitionVO vo = new RecognitionVO();
        parseArrayFallback(content, vo);
        if (vo.getIngredients().isEmpty()) {
            log.warn("识别结果无法解析: {}", content);
            throw new BusinessException("识别结果解析失败，请稍后重试或换一张图片");
        }
        return vo;
    }

    private void parseArrayFallback(String content, RecognitionVO vo) {
        try {
            JsonNode arr = objectMapper.readTree(LlmClient.extractJsonArray(content));
            if (arr != null && arr.isArray()) {
                fillNames(arr, vo.getIngredients(), 12);
            }
        } catch (Exception ignored) {
            // 保持空，交由上层判定
        }
    }

    private void fillNames(JsonNode arr, List<String> target, int limit) {
        if (arr == null || !arr.isArray()) {
            return;
        }
        arr.forEach(n -> {
            String name;
            if (n.isObject()) {
                // 兼容 [{"name":"番茄"}] 这种结构
                JsonNode nameNode = n.get("name");
                name = nameNode == null ? "" : nameNode.asText("").trim();
            } else {
                name = n.asText("").trim();
            }
            if (!name.isEmpty() && target.size() < limit && !target.contains(name)) {
                target.add(name);
            }
        });
    }
}
