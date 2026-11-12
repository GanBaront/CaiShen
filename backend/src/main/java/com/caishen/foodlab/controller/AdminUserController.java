package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.UserAdminUpdateDTO;
import com.caishen.foodlab.entity.User;
import com.caishen.foodlab.service.AdminTradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台用户管理
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminTradeService adminTradeService;

    @GetMapping
    public Result<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Integer status) {
        return Result.success(adminTradeService.userPage(page, size, keyword, status));
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody UserAdminUpdateDTO dto) {
        return Result.success(adminTradeService.userUpdate(id, dto));
    }
}
