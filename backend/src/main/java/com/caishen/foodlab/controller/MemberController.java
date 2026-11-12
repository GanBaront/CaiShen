package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.MemberOrderDTO;
import com.caishen.foodlab.entity.BillingRecord;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.MemberService;
import com.caishen.foodlab.vo.MemberStatusVO;
import com.caishen.foodlab.vo.PlanVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员中心：当前身份、套餐、购买下单、订阅与购买记录
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /** 当前身份 + 今日剩余次数 */
    @GetMapping("/member/status")
    public Result<MemberStatusVO> status() {
        return Result.success(memberService.status(AuthContext.requireUserId()));
    }

    /** 三个套餐（免费 / 单次 / 会员） */
    @GetMapping("/member/plans")
    public Result<List<PlanVO>> plans() {
        return Result.success(memberService.plans(AuthContext.getUserId()));
    }

    /** 创建会员 / 单次订单 */
    @PostMapping("/member/order")
    public Result<BillingRecord> createOrder(@RequestBody @Valid MemberOrderDTO dto) {
        return Result.success(memberService.createOrder(AuthContext.requireUserId(), dto));
    }

    /** 订阅与购买记录 */
    @GetMapping("/billing/records")
    public Result<List<BillingRecord>> billingRecords() {
        return Result.success(memberService.billingRecords(AuthContext.requireUserId()));
    }
}
