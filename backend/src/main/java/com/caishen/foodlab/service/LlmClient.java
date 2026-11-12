package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.config.AppProperties;
import com.caishen.foodlab.entity.SysConfig;
import com.caishen.foodlab.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型统一客户端：菜谱生成 / 推荐排序 / 图片识别等 AI 能力共用。
 * 调用 OpenAI 兼容的 /chat/completions 接口（DeepSeek、智谱 GLM、阿里百炼、Moonshot、OpenAI 等均兼容）。
 *
 * 文本与视觉使用**两套独立配置**，因为主流文本模型（如 DeepSeek）并不带视觉能力：
 * - 文本：ai.base-url / ai.model / ai.api-key            → 推荐排序、AI 创作菜谱
 * - 视觉：ai.vision-base-url / ai.vision-model / ai.vision-api-key → 图片识别菜品与食材
 *   视觉三项留空时自动复用文本配置（同一服务商可只配一套）。
 *
 * 配置解析优先级：
 * - 普通配置（地址 / 模型名）：sys_config（后台可运维修改）> application.yml（含环境变量）；
 * - **密钥：环境变量 > sys_config**，密钥属于部署期机密，不落库、不随源码包外发。
 * 占位值（如 api.example.com）视为未配置；未配置密钥时抛出明确错误，绝不伪造模型结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {

    private final SysConfigMapper sysConfigMapper;
    private final AppProperties appProperties;

    /** 一套完整的调用端点：地址 + 密钥 + 模型 + 图片格式 */
    private record Endpoint(String baseUrl, String apiKey, String model, String imageMode) {
    }

    /** 文本能力是否已配置（供需要预检/降级的调用方使用，不抛错） */
    public boolean isConfigured() {
        Endpoint ep = textEndpoint();
        return notBlank(ep.baseUrl()) && notBlank(ep.apiKey()) && notBlank(ep.model());
    }

    /** 视觉能力是否已配置 */
    public boolean isVisionConfigured() {
        Endpoint ep = visionEndpoint();
        return notBlank(ep.baseUrl()) && notBlank(ep.apiKey()) && notBlank(ep.model());
    }

    /** 当前生效的文本模型名（用于后台展示 / 日志） */
    public String currentTextModel() {
        return textEndpoint().model();
    }

    /** 当前生效的视觉模型名 */
    public String currentVisionModel() {
        return visionEndpoint().model();
    }

    /** 文本对话（使用 ai.model） */
    public String chat(String systemPrompt, String userPrompt, double temperature) {
        Endpoint ep = textEndpoint();
        return call(ep, systemPrompt, userPrompt, null, temperature);
    }

    /** 图像理解（使用 ai.vision-model）。imageDataUri 支持公网 http(s) URL 或 data:image/...;base64,... */
    public String chatWithImage(String systemPrompt, String userPrompt, String imageDataUri, double temperature) {
        Endpoint ep = visionEndpoint();
        return call(ep, systemPrompt, userPrompt, normalizeImage(ep, imageDataUri), temperature);
    }

    // ==================== 连通性自检（后台「测试连接」） ====================

    /**
     * 文本模型连通性测试：发一句极短的指令，返回模型回复与耗时。
     */
    public Map<String, Object> testText() {
        Map<String, Object> result = new LinkedHashMap<>();
        Endpoint ep = textEndpoint();
        result.put("configured", notBlank(ep.baseUrl()) && notBlank(ep.apiKey()));
        result.put("model", ep.model());
        result.put("baseUrl", ep.baseUrl());
        if (!Boolean.TRUE.equals(result.get("configured"))) {
            result.put("ok", false);
            result.put("message", "未配置文本模型密钥（ai.api-key / 环境变量 AI_API_KEY）");
            return result;
        }
        long start = System.currentTimeMillis();
        try {
            String reply = call(ep, "你是一个连通性测试助手，请只回复“连接正常”四个字。", "请回复“连接正常”。", null, 0.1);
            result.put("ok", true);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("reply", truncate(reply, 60));
        } catch (Exception e) {
            result.put("ok", false);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 视觉模型连通性测试：用一张 1x1 的纯色小图探测，只看接口是否连通、鉴权是否通过，
     * 不校验识别内容（避免依赖测试图片本身）。
     */
    public Map<String, Object> testVision() {
        Map<String, Object> result = new LinkedHashMap<>();
        Endpoint ep = visionEndpoint();
        result.put("configured", notBlank(ep.baseUrl()) && notBlank(ep.apiKey()));
        result.put("model", ep.model());
        result.put("baseUrl", ep.baseUrl());
        result.put("imageMode", resolveImageMode(ep));
        if (!Boolean.TRUE.equals(result.get("configured"))) {
            result.put("ok", false);
            result.put("message", "未配置视觉模型密钥（ai.vision-api-key / 环境变量 AI_VISION_API_KEY）");
            return result;
        }
        long start = System.currentTimeMillis();
        try {
            String reply = call(ep, "你是一个连通性测试助手，无论看到什么图片都只回复“连接正常”四个字。",
                    "这是一张测试图片，请回复“连接正常”。", normalizeImage(ep, TINY_PNG_DATA_URI), 0.1);
            result.put("ok", true);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("reply", truncate(reply, 60));
        } catch (Exception e) {
            result.put("ok", false);
            result.put("latencyMs", System.currentTimeMillis() - start);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 底层调用 ====================

    private String call(Endpoint ep, String systemPrompt, String userPrompt,
                        String imageDataUri, double temperature) {
        if (!notBlank(ep.apiKey())) {
            throw new BusinessException(imageDataUri == null
                    ? "未配置大模型 API Key：请在后台「系统配置」维护 ai.api-key，或设置环境变量 AI_API_KEY"
                    : "未配置视觉大模型 API Key：请维护 ai.vision-api-key（留空则复用 ai.api-key），或设置环境变量 AI_VISION_API_KEY");
        }
        if (!notBlank(ep.baseUrl())) {
            throw new BusinessException("未配置大模型接口地址：请在后台「系统配置」维护 ai.base-url / ai.vision-base-url");
        }
        if (!notBlank(ep.model())) {
            throw new BusinessException("未配置大模型名称（ai.model / ai.vision-model）");
        }
        String endpoint = stripTrailingSlash(ep.baseUrl());

        Object userContent;
        if (imageDataUri == null || imageDataUri.isBlank()) {
            userContent = userPrompt;
        } else {
            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("type", "text", "text", userPrompt));
            parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageDataUri)));
            userContent = parts;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", ep.model());
        body.put("temperature", temperature);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)));

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(Math.max(5, appProperties.getAi().getTimeoutSeconds()) * 1000);

        try {
            RestClient client = RestClient.builder().baseUrl(endpoint).requestFactory(factory).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + ep.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                throw new BusinessException("大模型未返回有效内容，请稍后重试");
            }
            return content;
        } catch (RestClientException e) {
            log.error("大模型调用失败 [{} @ {}]: {}", ep.model(), endpoint, e.getMessage());
            throw new BusinessException("大模型调用失败：" + e.getMessage());
        }
    }

    // ==================== 图片入参格式适配 ====================

    /**
     * 智谱 GLM 视觉模型只接受裸 base64，不接受 data:image/... 前缀；
     * 其余 OpenAI 兼容服务通常两种都认。
     */
    private String normalizeImage(Endpoint ep, String image) {
        if (image == null) {
            return null;
        }
        if (!"raw-base64".equals(resolveImageMode(ep))) {
            return image;
        }
        if (image.startsWith("data:")) {
            int comma = image.indexOf(',');
            return comma >= 0 ? image.substring(comma + 1) : image;
        }
        return image;
    }

    private String resolveImageMode(Endpoint ep) {
        String mode = ep.imageMode();
        if ("raw-base64".equalsIgnoreCase(mode) || "data-uri".equalsIgnoreCase(mode)) {
            return mode.toLowerCase();
        }
        // auto：按服务商特征判断
        String url = ep.baseUrl() == null ? "" : ep.baseUrl().toLowerCase();
        return url.contains("bigmodel.cn") || url.contains("zhipu") ? "raw-base64" : "data-uri";
    }

    // ==================== 配置解析 ====================

    private Endpoint textEndpoint() {
        String baseUrl = cfg("ai.base-url", appProperties.getAi().getBaseUrl());
        return new Endpoint(
                baseUrl,
                secret("ai.api-key", appProperties.getAi().getApiKey()),
                cfg("ai.model", appProperties.getAi().getModel()),
                cfg("ai.vision-image-mode", appProperties.getAi().getVisionImageMode()));
    }

    /** 视觉端点：专用配置优先，留空时回退文本配置（同一服务商只需配一套密钥） */
    private Endpoint visionEndpoint() {
        String baseUrl = cfg("ai.vision-base-url", appProperties.getAi().getVisionBaseUrl());
        if (!notBlank(baseUrl)) {
            baseUrl = cfg("ai.base-url", appProperties.getAi().getBaseUrl());
        }
        String apiKey = secret("ai.vision-api-key", appProperties.getAi().getVisionApiKey());
        if (!notBlank(apiKey)) {
            apiKey = secret("ai.api-key", appProperties.getAi().getApiKey());
        }
        return new Endpoint(
                baseUrl,
                apiKey,
                cfg("ai.vision-model", appProperties.getAi().getVisionModel()),
                cfg("ai.vision-image-mode", appProperties.getAi().getVisionImageMode()));
    }

    /**
     * 密钥解析：**环境变量优先**，未设置时才回落到后台「系统配置」。
     *
     * 与普通配置（DB 优先）相反，密钥刻意做成 env 优先：
     * 密钥属于部署期机密，不该落库、更不该跟着数据库备份或源码包到处跑；
     * 同时避免「env 改好了但库里留着旧 Key，实际仍用旧的」这种排障噩梦。
     */
    private String secret(String key, String envValue) {
        if (notBlank(envValue)) {
            return envValue.trim();
        }
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (config != null && notBlank(config.getConfigValue())) {
            String value = config.getConfigValue().trim();
            if (!isPlaceholder(value)) {
                return value;
            }
        }
        return "";
    }

    /** 优先取系统配置（后台可维护），空值/占位值回退到 application.yml / 环境变量 */
    private String cfg(String key, String fallback) {
        SysConfig config = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (config != null && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            String value = config.getConfigValue().trim();
            if (!isPlaceholder(value)) {
                return value;
            }
        }
        return fallback;
    }

    /** 占位符检测：示例地址 / 示例密钥一律视为未配置 */
    private boolean isPlaceholder(String value) {
        return value.contains("example.com") || value.equals("sk-xxx") || value.equals("your-api-key");
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        String t = text.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null || response.get("choices") == null) {
            return null;
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            return null;
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null || message.get("content") == null) {
            return null;
        }
        return String.valueOf(message.get("content"));
    }

    // ==================== 通用工具 ====================

    /** 1x1 透明 PNG，仅用于视觉模型连通性探测 */
    private static final String TINY_PNG_DATA_URI =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk"
                    + "+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    /**
     * 从模型返回内容中提取 JSON 文本：
     * 容忍 markdown 代码块包裹、前后解释性文字（截取首个 { 到最后一个 }）。
     */
    public static String extractJson(String content) {
        if (content == null) {
            return "";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd > 0) {
                text = text.substring(firstLineEnd + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text.trim();
    }

    /**
     * 从模型返回内容中提取 JSON 数组：优先整体解析数组，
     * 兼容被包在 {"xxx":[...]} 里或带 ``` 代码块的情况。
     */
    public static String extractJsonArray(String content) {
        if (content == null) {
            return "";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd > 0) {
                text = text.substring(firstLineEnd + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) {
                text = text.substring(0, fence);
            }
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        return text.trim();
    }
}
