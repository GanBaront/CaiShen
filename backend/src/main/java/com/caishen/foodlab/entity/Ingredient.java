package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 食材库
 */
@Data
@TableName("t_ingredient")
public class Ingredient {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 食材名 */
    private String name;

    /** 别名(逗号分隔) */
    private String alias;

    /** 分类:蔬菜/肉类/蛋类/调味/主食/其他 */
    private String category;

    /** 默认单位 */
    private String defaultUnit;

    private String image;

    private Integer status;

    private LocalDateTime createTime;
}
