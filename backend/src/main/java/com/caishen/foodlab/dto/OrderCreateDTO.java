package com.caishen.foodlab.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建订单入参
 */
@Data
public class OrderCreateDTO {

    private Long merchantId;

    /** 为空时取当前用户购物清单中未勾选的食材 */
    private List<Item> items;

    @Data
    public static class Item {
        private Long ingredientId;
        private String name;
        private String amount;
        private String unit;
        private BigDecimal price;
    }
}
