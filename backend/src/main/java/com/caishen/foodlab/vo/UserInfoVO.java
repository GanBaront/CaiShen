package com.caishen.foodlab.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息
 */
@Data
public class UserInfoVO {

    private Long id;

    private String account;

    private String nickname;

    private String phone;

    private String avatar;

    private Integer status;

    /** 是否会员 */
    private Boolean member;

    /** 会员到期时间（非会员为空） */
    private LocalDateTime memberEndTime;
}
