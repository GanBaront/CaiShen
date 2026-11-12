package com.caishen.foodlab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册入参（账号 + 密码）
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "请输入账号")
    private String account;

    @NotBlank(message = "请输入密码")
    private String password;

    private String nickname;

    /** 图形验证码 id（由 /api/auth/captcha 生成） */
    private String captchaId;

    /** 用户输入的图形验证码 */
    private String captchaCode;
}
