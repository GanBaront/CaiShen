package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.AdminLoginDTO;
import com.caishen.foodlab.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端登录 / 退出
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
