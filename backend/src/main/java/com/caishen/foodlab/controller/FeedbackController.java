package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.FeedbackCreateDTO;
import com.caishen.foodlab.entity.Feedback;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 意见反馈：提交 / 我的反馈
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Result<Feedback> create(@RequestBody @Valid FeedbackCreateDTO dto) {
        return Result.success(feedbackService.create(AuthContext.requireUserId(), dto));
    }

    @GetMapping
    public Result<List<Feedback>> mine() {
        return Result.success(feedbackService.mine(AuthContext.requireUserId()));
    }
}
