package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.OrderCreateDTO;
import com.caishen.foodlab.dto.OrderReviewDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.OrderService;
import com.caishen.foodlab.vo.OrderListVO;
import com.caishen.foodlab.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单：下单、我的订单、详情、评价
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 全部加入购物车 → 下单 */
    @PostMapping
    public Result<OrderVO> create(@RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(AuthContext.requireUserId(), dto));
    }

    /** 我的订单（列表 + 状态计数） */
    @GetMapping
    public Result<OrderListVO> list() {
        return Result.success(orderService.list(AuthContext.requireUserId()));
    }

    /** 订单详情（查看进度） */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(AuthContext.requireUserId(), id));
    }

    /** 确认收货（已支付 / 配送中 → 已完成） */
    @PostMapping("/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        orderService.confirmReceive(AuthContext.requireUserId(), id);
        return Result.success();
    }

    /** 去评价 */
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody OrderReviewDTO dto) {
        orderService.review(AuthContext.requireUserId(), id, dto);
        return Result.success();
    }
}
