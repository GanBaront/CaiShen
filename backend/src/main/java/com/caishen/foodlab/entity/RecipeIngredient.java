package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜谱所需食材
 */
@Data
@TableName("t_recipe_ingredient")
public class RecipeIngredient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipeId;

    private Long ingredientId;

    private String name;

    /** 数量(如 2 / 少许) */
    private String amount;

    private String unit;

    /** 1主料 */
    private Integer isMain;

    /** 1必备 0可替代 */
    private Integer isRequired;

    private Integer sort;
}
