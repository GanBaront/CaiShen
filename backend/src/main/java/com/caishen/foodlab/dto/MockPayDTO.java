package com.caishen.foodlab.dto;

import lombok.Data;

/**
 * 模拟支付入参（购买记录、商城订单 或 直接指定购买类型，三者其一）
 */
@Data
public class MockPayDTO {

    private Long billingRecordId;

    private Long orderId;

    /** 直接按类型支付（会员·1个月 / 单次AI菜谱）：支付成功时才生成购买记录 */
    private String type;
}
