package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物清单
 */
@Data
@TableName("t_shopping_list")
public class ShoppingList {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    /** 来源菜谱(逗号分隔) */
    private String sourceRecipes;

    private LocalDateTime createTime;
}
