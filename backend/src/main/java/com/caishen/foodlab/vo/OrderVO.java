package com.caishen.foodlab.vo;

import com.caishen.foodlab.entity.OrderItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单返回
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    private Long merchantId;

    private String merchantName;

    private BigDecimal amount;

    private String status;

    private String deliveryDesc;

    private LocalDateTime createTime;

    /** 是否已评价 */
    private Boolean reviewed = false;

    private List<OrderItem> items = new ArrayList<>();
}
