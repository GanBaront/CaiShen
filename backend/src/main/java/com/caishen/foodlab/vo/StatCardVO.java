package com.caishen.foodlab.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 后台统计卡（数值 + 环比）
 */
@Data
public class StatCardVO {

    private BigDecimal value;

    /** 较上周变化百分比 */
    private BigDecimal deltaPercent;

    /** up / down */
    private String trend;

    public StatCardVO() {
    }

    public StatCardVO(BigDecimal value, BigDecimal deltaPercent) {
        this.value = value;
        this.deltaPercent = deltaPercent;
        this.trend = deltaPercent.signum() >= 0 ? "up" : "down";
    }

    public static StatCardVO of(long value, BigDecimal deltaPercent) {
        return new StatCardVO(BigDecimal.valueOf(value), deltaPercent);
    }
}
