package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.service.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 能力就绪状态：前端据此决定是否展示「识别 / 推荐」入口，
 * 以及未配置模型时给出明确提示（而不是让用户点了才看到报错）。
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiStatusController {

    private final LlmClient llmClient;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("textEnabled", llmClient.isConfigured());
        result.put("visionEnabled", llmClient.isVisionConfigured());
        result.put("textModel", llmClient.currentTextModel());
        result.put("visionModel", llmClient.currentVisionModel());
        return Result.success(result);
    }
}
