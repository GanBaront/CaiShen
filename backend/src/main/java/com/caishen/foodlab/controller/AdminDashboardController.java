package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台数据概览
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    /** 时间范围只支持近 7 / 30 天，其余按 7 天处理 */
    private int normalizeRange(int range) {
        return range == 30 ? 30 : 7;
    }

    /** 4 统计卡（range = 近 N 天，环比上一期） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(name = "range", defaultValue = "7") int range) {
        return Result.success(dashboardService.overview(normalizeRange(range)));
    }

    /** AI 菜谱生成趋势（近 N 天折线） */
    @GetMapping("/ai-trend")
    public Result<Map<String, Object>> aiTrend(@RequestParam(name = "range", defaultValue = "7") int range) {
        return Result.success(dashboardService.aiTrend(normalizeRange(range)));
    }

    /** 待处理审核计数 */
    @GetMapping("/pending")
    public Result<Map<String, Object>> pending() {
        return Result.success(dashboardService.pending());
    }
}
