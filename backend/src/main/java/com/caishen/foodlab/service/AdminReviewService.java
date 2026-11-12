package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.entity.Feedback;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeReview;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.mapper.FeedbackMapper;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.RecipeReviewMapper;
import com.caishen.foodlab.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台审核与反馈：评论审核、用户反馈处理
 */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private static final List<String> REVIEW_STATUS = List.of("待审核", "已通过", "已驳回");

    private final RecipeReviewMapper recipeReviewMapper;
    private final RecipeMapper recipeMapper;
    private final UserMapper userMapper;
    private final FeedbackMapper feedbackMapper;

    // ==================== 评论审核 ====================

    public PageResult<Map<String, Object>> commentPage(long page, long size, String status, Long recipeId) {
        LambdaQueryWrapper<RecipeReview> wrapper = new LambdaQueryWrapper<RecipeReview>()
                .eq(status != null && !status.isBlank(), RecipeReview::getStatus, status)
                .eq(recipeId != null, RecipeReview::getRecipeId, recipeId)
                .orderByDesc(RecipeReview::getCreateTime)
                .orderByDesc(RecipeReview::getId);
        Page<RecipeReview> reviewPage = recipeReviewMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, User> userMap = new HashMap<>();
        Map<Long, Recipe> recipeMap = new HashMap<>();
        if (!reviewPage.getRecords().isEmpty()) {
            List<Long> userIds = reviewPage.getRecords().stream().map(RecipeReview::getUserId)
                    .distinct().collect(Collectors.toList());
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
            List<Long> recipeIds = reviewPage.getRecords().stream().map(RecipeReview::getRecipeId)
                    .distinct().collect(Collectors.toList());
            for (Recipe recipe : recipeMapper.selectBatchIds(recipeIds)) {
                recipeMap.put(recipe.getId(), recipe);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (RecipeReview review : reviewPage.getRecords()) {
            User user = userMap.get(review.getUserId());
            Recipe recipe = recipeMap.get(review.getRecipeId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", review.getId());
            row.put("userId", review.getUserId());
            row.put("nickname", user == null ? null : user.getNickname());
            row.put("account", user == null ? null : user.getAccount());
            row.put("recipeId", review.getRecipeId());
            row.put("recipeName", recipe == null ? null : recipe.getName());
            row.put("rating", review.getRating());
            row.put("tags", review.getTags());
            row.put("content", review.getContent());
            row.put("status", review.getStatus());
            row.put("createTime", review.getCreateTime());
            list.add(row);
        }
        return PageResult.of(reviewPage.getTotal(), reviewPage.getCurrent(), reviewPage.getSize(), list);
    }

    /** 评论审核通过 / 驳回 */
    public RecipeReview commentAudit(Long id, String status) {
        if (!REVIEW_STATUS.contains(status)) {
            throw new BusinessException("非法的审核状态");
        }
        if (recipeReviewMapper.selectById(id) == null) {
            throw new BusinessException("评论不存在");
        }
        RecipeReview update = new RecipeReview();
        update.setId(id);
        update.setStatus(status);
        recipeReviewMapper.updateById(update);
        return recipeReviewMapper.selectById(id);
    }

    // ==================== 用户反馈 ====================

    public PageResult<Map<String, Object>> feedbackPage(long page, long size, String status) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<Feedback>()
                .eq(status != null && !status.isBlank(), Feedback::getStatus, status)
                .orderByDesc(Feedback::getCreateTime)
                .orderByDesc(Feedback::getId);
        Page<Feedback> feedbackPage = feedbackMapper.selectPage(new Page<>(page, size), wrapper);

        Map<Long, User> userMap = new HashMap<>();
        List<Long> userIds = feedbackPage.getRecords().stream().map(Feedback::getUserId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Feedback feedback : feedbackPage.getRecords()) {
            User user = userMap.get(feedback.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", feedback.getId());
            row.put("userId", feedback.getUserId());
            row.put("account", user == null ? null : user.getAccount());
            row.put("nickname", user == null ? null : user.getNickname());
            row.put("content", feedback.getContent());
            row.put("contact", feedback.getContact());
            row.put("status", feedback.getStatus());
            row.put("createTime", feedback.getCreateTime());
            list.add(row);
        }
        return PageResult.of(feedbackPage.getTotal(), feedbackPage.getCurrent(), feedbackPage.getSize(), list);
    }

    /** 处理用户反馈 */
    public Feedback feedbackHandle(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        Feedback update = new Feedback();
        update.setId(id);
        update.setStatus("已处理");
        feedbackMapper.updateById(update);
        return feedbackMapper.selectById(id);
    }
}
