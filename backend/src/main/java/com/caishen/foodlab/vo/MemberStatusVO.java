package com.caishen.foodlab.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会员状态返回
 */
@Data
public class MemberStatusVO {

    /** 免费/会员 */
    private String planType;

    private Boolean member;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer limit;

    private Integer used;

    private Integer remaining;
}
