package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我的食材篮（首页录入的当前食材）
 */
@Data
@TableName("t_user_ingredient")
public class UserIngredient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联食材库 */
    private Long ingredientId;

    private String name;

    private String quantity;

    private String unit;

    private LocalDateTime createTime;
}
