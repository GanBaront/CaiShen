package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.MerchantService;
import com.caishen.foodlab.vo.MerchantCatalogVO;
import com.caishen.foodlab.vo.MerchantCompareVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家比价：平台内商家比价 + 查看商家明细 + 可购商品总清单
 */
@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /** 商家比价（sort：综合 / 价格最低 / 好评率 / 距离最近） */
    @GetMapping("/compare")
    public Result<List<MerchantCompareVO>> compare(@RequestParam(required = false) String sort,
                                                   @RequestParam(required = false) List<String> names) {
        return Result.success(merchantService.compare(AuthContext.getUserId(), sort, names));
    }

    /** 查看商家明细（报价单） */
    @GetMapping("/{id}/quote")
    public Result<MerchantCompareVO> quote(@PathVariable Long id,
                                           @RequestParam(required = false) List<String> names) {
        return Result.success(merchantService.quote(id, AuthContext.getUserId(), names));
    }

    /** 商家可购商品总清单（能看到平台商家一共可购买的东西） */
    @GetMapping("/products")
    public Result<List<MerchantCatalogVO>> products() {
        return Result.success(merchantService.catalog());
    }
}
