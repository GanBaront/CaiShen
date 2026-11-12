package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 购物清单新增食材入参（含「添加其他食材」）
 */
@Data
public class ShoppingListItemAddDTO {

    private String name;

    private String amount;

    private String unit;

    /** 分组:调味/蔬菜/肉类/其他 */
    private String category;

    private Long ingredientId;
}
