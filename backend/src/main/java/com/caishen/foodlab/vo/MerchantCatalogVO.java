package com.caishen.foodlab.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家可购商品总清单：按商品名聚合所有已上架商家的报价
 */
@Data
public class MerchantCatalogVO {

    /** 商品名（食材名） */
    private String name;

    private Long ingredientId;

    /** 售卖规格（购买单位） */
    private String sellUnit;

    /** 最低价 */
    private BigDecimal minPrice;

    /** 最高价 */
    private BigDecimal maxPrice;

    /** 在售商家数 */
    private Integer merchantCount = 0;

    /** 各商家报价 */
    private List<Offer> offers = new ArrayList<>();

    @Data
    public static class Offer {
        private Long merchantId;
        private String merchantName;
        private BigDecimal price;
        private Integer stock;
        private Integer rating;
    }
}
