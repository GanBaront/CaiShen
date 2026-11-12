package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 食材-过敏源映射
 */
@Data
@TableName("t_ingredient_allergen")
public class IngredientAllergen {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ingredientId;

    private Long allergenTagId;
}
