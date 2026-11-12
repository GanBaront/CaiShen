package com.caishen.foodlab.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜谱推荐列表查询入参（个性化筛选 + 排序 + 分页）
 */
@Data
public class RecipeQueryDTO {

    private List<String> tastes;

    private String dietType;

    private String difficulty;

    /** 最大烹饪耗时(分钟) */
    private Integer maxCookTime;

    private Integer servings;

    private List<String> avoidItems;

    /** 我的食材（用于匹配度计算与过滤） */
    private List<String> ingredients;

    /** 匹配度 / 耗时 / 难度 */
    private String sort = "匹配度";

    private Long page = 1L;

    private Long size = 12L;
}
