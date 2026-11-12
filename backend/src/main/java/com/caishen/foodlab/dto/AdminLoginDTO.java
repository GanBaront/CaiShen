package com.caishen.foodlab.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员登录入参
 */
@Data
public class AdminLoginDTO {

    @NotBlank(message = "请输入管理员账号")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;
}
