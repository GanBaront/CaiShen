package com.caishen.foodlab.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录/注册返回（令牌 + 用户信息）
 */
@Data
public class AuthVO {

    private String token;

    private UserInfoVO user;
}
