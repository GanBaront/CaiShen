package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物清单项
 */
@Data
@TableName("t_shopping_list_item")
public class ShoppingListItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long listId;

    private Long ingredientId;

    private String name;

    private String amount;

    private String unit;

    /** 分组:调味/蔬菜/肉类/其他 */
    private String category;

    /** 1已购 */
    private Integer checked;

    /** 来自哪个菜谱 */
    private String recipeSource;

    private Integer sort;

    /** 商家售卖规格（购买单位，展示用，不入库） */
    @TableField(exist = false)
    private String sellUnit;

    /** 商家最低参考价（展示用，不入库） */
    @TableField(exist = false)
    private BigDecimal bestPrice;
}
