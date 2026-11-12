package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("t_orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long merchantId;

    private String merchantName;

    private BigDecimal amount;

    /** 待支付/已支付/配送中/已完成 */
    private String status;

    private String deliveryDesc;

    private LocalDateTime createTime;
}
