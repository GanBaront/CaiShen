package com.caishen.foodlab.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台菜谱新增/编辑入参
 */
@Data
public class RecipeSaveDTO {

    private String name;

    private String coverImage;

    private String cuisine;

    private String difficulty;

    private String taste;

    private Integer cookTime;

    private String description;

    private Integer servings;

    private String source;

    private String status;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbs;
}
