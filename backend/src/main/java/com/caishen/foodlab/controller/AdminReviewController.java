package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.StatusUpdateDTO;
import com.caishen.foodlab.entity.Feedback;
import com.caishen.foodlab.entity.RecipeReview;
import com.caishen.foodlab.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台审核与反馈：评论审核、用户反馈处理
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping("/comments")
    public Result<PageResult<Map<String, Object>>> commentPage(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String status,
                                                               @RequestParam(required = false) Long recipeId) {
        return Result.success(adminReviewService.commentPage(page, size, status, recipeId));
    }

    /** 评论审核通过 / 驳回 */
    @PutMapping("/comments/{id}/audit")
    public Result<RecipeReview> commentAudit(@PathVariable Long id, @RequestBody StatusUpdateDTO dto) {
        return Result.success(adminReviewService.commentAudit(id, dto.getStatus()));
    }

    @GetMapping("/feedback")
    public Result<PageResult<Map<String, Object>>> feedbackPage(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "10") long size,
                                                                @RequestParam(required = false) String status) {
        return Result.success(adminReviewService.feedbackPage(page, size, status));
    }

    /** 处理用户反馈 */
    @PutMapping("/feedback/{id}/handle")
    public Result<Feedback> feedbackHandle(@PathVariable Long id) {
        return Result.success(adminReviewService.feedbackHandle(id));
    }
}
