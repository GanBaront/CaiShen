package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.LoginDTO;
import com.caishen.foodlab.dto.RegisterDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.AuthService;
import com.caishen.foodlab.service.CaptchaService;
import com.caishen.foodlab.vo.AuthVO;
import com.caishen.foodlab.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证：注册 / 登录 / 退出 / 当前用户 / 图形验证码
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /** 获取图形验证码（登录 / 注册人机验证用） */
    @GetMapping("/captcha")
    public Result<CaptchaService.CaptchaVO> captcha() {
        return Result.success(captchaService.generate());
    }

    @PostMapping("/register")
    public Result<AuthVO> register(@RequestBody @Valid RegisterDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@RequestBody @Valid LoginDTO dto) {
        captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode());
        return Result.success(authService.login(dto));
    }

    /** 无状态 JWT，退出登录由前端清除令牌即可 */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.me(AuthContext.requireUserId()));
    }
}
