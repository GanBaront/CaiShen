package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营养数据（每 100g）
 */
@Data
@TableName("t_ingredient_nutrition")
public class IngredientNutrition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ingredientId;

    /** 热量 kcal */
    private BigDecimal calories;

    /** 蛋白质 g */
    private BigDecimal protein;

    /** 脂肪 g */
    private BigDecimal fat;

    /** 碳水 g */
    private BigDecimal carbs;

    /** 说明，如 每100g */
    private String unitDesc;

    private LocalDateTime updateTime;
}
