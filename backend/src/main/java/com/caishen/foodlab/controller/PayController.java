package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.MockPayDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.PayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模拟支付：不接真实商户，仅做状态流转
 */
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /** 模拟支付（会员/单次购买记录 或 商城订单） */
    @PostMapping("/mock")
    public Result<Map<String, Object>> mockPay(@RequestBody MockPayDTO dto) {
        return Result.success(payService.mockPay(AuthContext.requireUserId(), dto));
    }
}
