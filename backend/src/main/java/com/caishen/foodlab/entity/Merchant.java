package com.caishen.foodlab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家
 */
@Data
@TableName("t_merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String logo;

    /** 好评率(%) */
    private Integer rating;

    private BigDecimal distanceKm;

    /** 约送达(分钟) */
    private Integer deliveryMinutes;

    /** 标签:综合推荐/价格最低 */
    private String tags;

    /** 库存充足/库存紧张 */
    private String stockStatus;

    /** 待审核/已通过/已驳回 */
    private String auditStatus;

    private Integer status;

    private LocalDateTime createTime;
}
