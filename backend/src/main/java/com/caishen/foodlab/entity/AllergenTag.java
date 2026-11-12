package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 过敏源标签
 */
@Data
@TableName("t_allergen_tag")
public class AllergenTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String remark;

    private Integer status;
}
