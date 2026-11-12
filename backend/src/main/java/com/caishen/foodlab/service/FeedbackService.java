package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.dto.FeedbackCreateDTO;
import com.caishen.foodlab.entity.Feedback;
import com.caishen.foodlab.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 意见反馈
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;

    /** 提交意见反馈 */
    public Feedback create(Long userId, FeedbackCreateDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContent(dto.getContent().trim());
        feedback.setContact(dto.getContact());
        feedback.setStatus("待处理");
        feedbackMapper.insert(feedback);
        return feedback;
    }

    /** 我的反馈 / 提交记录 */
    public List<Feedback> mine(Long userId) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreateTime));
    }
}
