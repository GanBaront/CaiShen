package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜谱
 */
@Data
@TableName("t_recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String coverImage;

    /** 菜系:家常菜/创意菜/轻食 */
    private String cuisine;

    /** 难度:简单/中等/较难 */
    private String difficulty;

    /** 口味 */
    private String taste;

    /** 耗时(分钟) */
    private Integer cookTime;

    private String description;

    /** 份量(人份) */
    private Integer servings;

    /** 来源:人工/AI生成 */
    private String source;

    /** 状态:已上线/待审核/已下架 */
    private String status;

    /** 每份热量 kcal */
    private BigDecimal calories;

    private BigDecimal protein;

    private BigDecimal fat;

    private BigDecimal carbs;

    private LocalDateTime createTime;
}
