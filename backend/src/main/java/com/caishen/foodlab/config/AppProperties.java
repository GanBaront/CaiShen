package com.caishen.foodlab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 业务自定义配置项（支持环境变量覆盖）
 */
@Data
@Component
@ConfigurationProperties(prefix = "caishen")
public class AppProperties {

    /** 每周免费生成次数（自然周） */
    private int freeDailyLimit = 5; // 语义已改为每周免费次数

    /** 会员月费(元) */
    private BigDecimal memberMonthPrice = new BigDecimal("19");

    /** 单次 AI 菜谱费用(元) */
    private BigDecimal aiSinglePrice = new BigDecimal("2.99");

    /** 图片上传保存目录 */
    private String uploadDir = "uploads";

    /** 图片可访问 URL 前缀 */
    private String uploadUrlPrefix = "/uploads";

    /** 大模型配置 */
    private Ai ai = new Ai();

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    @Data
    public static class Ai {
        /** 文本模型 OpenAI 兼容接口地址（默认 DeepSeek） */
        private String baseUrl = "https://api.deepseek.com/v1";
        /** 文本大模型名称（菜谱生成 / 推荐排序） */
        private String model = "deepseek-chat";
        /** 文本模型密钥（默认空，未配置时 AI 接口返回明确错误，不伪造结果） */
        private String apiKey = "";

        /** 视觉模型接口地址（默认智谱 GLM；留空则复用 baseUrl） */
        private String visionBaseUrl = "https://open.bigmodel.cn/api/paas/v4";
        /** 视觉大模型名称（图片识别食材/菜品） */
        private String visionModel = "glm-4v-flash";
        /** 视觉模型密钥（留空则复用 apiKey） */
        private String visionApiKey = "";
        /**
         * 视觉模型图片入参格式：auto | data-uri | raw-base64。
         * auto 按服务商判断（智谱只认裸 base64，不支持 data:image/... 前缀），
         * 遇到其它兼容服务可显式指定 data-uri。
         */
        private String visionImageMode = "auto";

        /** 请求超时(秒) */
        private int timeoutSeconds = 60;
    }

    @Data
    public static class Jwt {
        /** 签名密钥 */
        private String secret = "caishen-foodlab-local-dev-secret-key-please-change-2026";
        /** 过期时间(小时) */
        private int expireHours = 168;
    }
}
