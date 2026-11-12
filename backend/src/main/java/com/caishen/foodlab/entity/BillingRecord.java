package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅与购买记录
 */
@Data
@TableName("t_billing_record")
public class BillingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 单次AI菜谱/会员·1个月 */
    private String type;

    private BigDecimal amount;

    /** 模拟支付 */
    private String payMethod;

    /** 已支付/待支付 */
    private String status;

    private LocalDateTime createTime;
}
