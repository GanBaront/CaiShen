package com.caishen.foodlab.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家比价返回
 */
@Data
public class MerchantCompareVO {

    private Long id;

    private String name;

    private String logo;

    private Integer rating;

    private BigDecimal distanceKm;

    private Integer deliveryMinutes;

    private String tags;

    private String stockStatus;

    /** 覆盖清单的总价 */
    private BigDecimal totalPrice = BigDecimal.ZERO;

    /** 覆盖清单项数 */
    private Integer coveredCount = 0;

    /** 清单总项数 */
    private Integer totalCount = 0;

    /** 缺失的食材名 */
    private List<String> missingNames = new ArrayList<>();

    /** 是否全部覆盖 */
    private Boolean fullCoverage = false;

    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private Long ingredientId;
        private String name;
        private String amount;
        private String unit;
        /** 商家售卖规格（购买单位） */
        private String sellUnit;
        private BigDecimal price;
        private Integer stock;
    }
}
