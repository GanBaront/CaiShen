package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 我的订单列表（含状态计数）
 */
@Data
public class OrderListVO {

    private List<OrderVO> list;

    /** 状态计数：待支付/已支付/配送中/已完成 */
    private Map<String, Integer> statusCount;

    private Integer total;
}
