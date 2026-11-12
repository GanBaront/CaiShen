package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.PageResult;
import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.AuditDTO;
import com.caishen.foodlab.entity.Merchant;
import com.caishen.foodlab.entity.MerchantProduct;
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
 * 后台会员与订单、商家审核、商品审核
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminTradeController {

    private final AdminTradeService adminTradeService;

    // ==================== 会员与订单 ====================

    @GetMapping("/orders")
    public Result<PageResult<Map<String, Object>>> orderPage(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "10") long size,
                                                             @RequestParam(required = false) String status) {
        return Result.success(adminTradeService.orderPage(page, size, status));
    }

    @GetMapping("/members")
    public Result<PageResult<Map<String, Object>>> memberPage(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "10") long size) {
        return Result.success(adminTradeService.memberPage(page, size));
    }

    // ==================== 商家审核 ====================

    @GetMapping("/merchants")
    public Result<PageResult<Merchant>> merchantPage(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String auditStatus,
                                                     @RequestParam(required = false) String keyword) {
        return Result.success(adminTradeService.merchantPage(page, size, auditStatus, keyword));
    }

    @PutMapping("/merchants/{id}/audit")
    public Result<Merchant> merchantAudit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        return Result.success(adminTradeService.merchantAudit(id, dto.getAuditStatus()));
    }

    // ==================== 商品审核 ====================

    @GetMapping("/products")
    public Result<PageResult<Map<String, Object>>> productPage(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "10") long size,
                                                               @RequestParam(required = false) String auditStatus) {
        return Result.success(adminTradeService.productPage(page, size, auditStatus));
    }

    @PutMapping("/products/{id}/audit")
    public Result<MerchantProduct> productAudit(@PathVariable Long id, @RequestBody AuditDTO dto) {
        return Result.success(adminTradeService.productAudit(id, dto.getAuditStatus()));
    }
}
