package com.caishen.foodlab.dto;

import lombok.Data;

/** 菜谱评价提交（进入待审核，管理端审核通过后对所有人展示） */
@Data
public class RecipeReviewDTO {

    /** 1-5 星 */
    private Integer rating;

    /** 评价内容（选填） */
    private String content;
}
