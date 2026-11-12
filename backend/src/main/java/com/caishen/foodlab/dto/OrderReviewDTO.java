package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 订单评价入参
 */
@Data
public class OrderReviewDTO {

    private Integer rating;

    private String content;
}
