package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜谱评价/评论
 */
@Data
@TableName("t_recipe_review")
public class RecipeReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long recipeId;

    /** 1-5星 */
    private Integer rating;

    /** 评价标签(选填) */
    private String tags;

    private String content;

    /** 待审核/已通过/已驳回 */
    private String status;

    private LocalDateTime createTime;
}
