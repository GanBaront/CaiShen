package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 购物清单项修改入参（勾选已购 / 修改数量）
 */
@Data
public class ShoppingListItemUpdateDTO {

    private Integer checked;

    private String amount;
}
