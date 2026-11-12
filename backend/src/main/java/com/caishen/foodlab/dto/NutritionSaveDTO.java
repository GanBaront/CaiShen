package com.caishen.foodlab.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台营养数据保存入参
 */
@Data
public class NutritionSaveDTO {

    private Long ingredientId;

    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbs;

    private String unitDesc;
}
