package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 食材替代
 */
@Data
@TableName("t_ingredient_substitute")
public class IngredientSubstitute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipeIngredientId;

    private Long ingredientId;

    private String name;

    /** 替代比例 */
    private String ratioDesc;

    /** 口味衔接 */
    private String note;
}
