package com.caishen.foodlab.vo;

import lombok.Data;

/**
 * 今日免费次数返回（如 3/5）
 */
@Data
public class FreeQuotaVO {

    /** 每周上限 */
    private Integer limit;

    /** 今日已用 */
    private Integer used;

    /** 今日剩余 */
    private Integer remaining;

    /** 是否会员（会员不限次） */
    private Boolean member;
}
