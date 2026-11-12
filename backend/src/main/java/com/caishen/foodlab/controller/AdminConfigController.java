package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.entity.SysConfig;
import com.caishen.foodlab.service.AdminConfigService;
import com.caishen.foodlab.service.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台系统配置：查询 / 修改 / AI 连通性自检
 */
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;
    private final LlmClient llmClient;

    @GetMapping
    public Result<List<SysConfig>> list() {
        return Result.success(adminConfigService.list());
    }

    @PutMapping
    public Result<List<SysConfig>> update(@RequestBody Map<String, String> configs) {
        return Result.success(adminConfigService.update(configs));
    }

    /**
     * AI 连通性测试：分别探测文本模型（默认 DeepSeek）与视觉模型（默认智谱 GLM），
     * 返回是否已配置、模型名、耗时与模型回复，便于在后台一眼看出密钥 / 地址是否可用。
     */
    @PostMapping("/ai-test")
    public Result<Map<String, Object>> aiTest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", llmClient.testText());
        result.put("vision", llmClient.testVision());
        return Result.success(result);
    }
}
