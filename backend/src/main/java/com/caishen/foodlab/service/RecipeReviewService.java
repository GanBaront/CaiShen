package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.RecipeReviewDTO;
import com.caishen.foodlab.entity.Recipe;
import com.caishen.foodlab.entity.RecipeReview;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.mapper.RecipeMapper;
import com.caishen.foodlab.mapper.RecipeReviewMapper;
import com.caishen.foodlab.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜谱评价：用户端提交（待审核）+ 已通过评论列表。
 * 与后台「评论审核」共用 t_recipe_review：管理员通过后才在用户端展示。
 */
@Service
@RequiredArgsConstructor
public class RecipeReviewService {

    private static final String RECIPE_ONLINE = "已上线";
    private static final String REVIEW_APPROVED = "已通过";
    private static final String REVIEW_PENDING = "待审核";

    private final RecipeReviewMapper recipeReviewMapper;
    private final RecipeMapper recipeMapper;
    private final UserMapper userMapper;

    /** 已通过的评价列表（用户端菜谱详情展示） */
    public List<Map<String, Object>> listByRecipe(Long recipeId) {
        List<RecipeReview> reviews = recipeReviewMapper.selectList(new LambdaQueryWrapper<RecipeReview>()
                .eq(RecipeReview::getRecipeId, recipeId)
                .eq(RecipeReview::getStatus, REVIEW_APPROVED)
                .orderByDesc(RecipeReview::getCreateTime)
                .orderByDesc(RecipeReview::getId));
        if (reviews.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, User> userMap = new HashMap<>();
        List<Long> userIds = reviews.stream().map(RecipeReview::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (RecipeReview review : reviews) {
            User user = userMap.get(review.getUserId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", review.getId());
            row.put("nickname", user == null ? "美食爱好者" : user.getNickname());
            row.put("rating", review.getRating());
            row.put("content", review.getContent());
            row.put("createTime", review.getCreateTime());
            list.add(row);
        }
        return list;
    }

    /** 提交评价：进入待审核，管理端审核通过后对所有人可见 */
    public void submit(Long userId, Long recipeId, RecipeReviewDTO dto) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null || !RECIPE_ONLINE.equals(recipe.getStatus())) {
            throw new BusinessException("菜谱不存在或已下架");
        }
        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("请给出 1-5 星评分");
        }
        if (recipeReviewMapper.selectCount(new LambdaQueryWrapper<RecipeReview>()
                .eq(RecipeReview::getUserId, userId)
                .eq(RecipeReview::getRecipeId, recipeId)) > 0) {
            throw new BusinessException("你已评价过这道菜");
        }
        RecipeReview review = new RecipeReview();
        review.setUserId(userId);
        review.setRecipeId(recipeId);
        review.setRating(dto.getRating());
        review.setContent(dto.getContent());
        review.setStatus(REVIEW_PENDING);
        review.setCreateTime(LocalDateTime.now());
        recipeReviewMapper.insert(review);
    }
}
