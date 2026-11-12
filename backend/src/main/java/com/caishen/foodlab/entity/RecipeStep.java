package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 分步教学（图文）
 */
@Data
@TableName("t_recipe_step")
public class RecipeStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipeId;

    private Integer stepNo;

    /** 步骤标题，如 备料/打蛋 */
    private String title;

    private String description;

    /** 步骤示意图 */
    private String imageUrl;

    /** 关键提示 */
    private String tips;

    private Integer sort;
}
