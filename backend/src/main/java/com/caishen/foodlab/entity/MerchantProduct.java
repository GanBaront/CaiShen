package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家商品报价
 */
@Data
@TableName("t_merchant_product")
public class MerchantProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private Long ingredientId;

    private String name;

    private BigDecimal price;

    /** 售卖规格（如 1 桶(900ml)、500g），下单与展示购买单位以此为准 */
    private String sellUnit;

    private Integer stock;

    /** 商品上架审核:待审核/已通过/已驳回 */
    private String auditStatus;

    private Integer status;
}
