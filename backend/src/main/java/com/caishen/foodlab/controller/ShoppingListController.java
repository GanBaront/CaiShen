package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.ShoppingListItemAddDTO;
import com.caishen.foodlab.dto.ShoppingListItemUpdateDTO;
import com.caishen.foodlab.entity.ShoppingListItem;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.ShoppingListService;
import com.caishen.foodlab.vo.ShoppingListVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 购物清单：查询、增改删、分享、导出
 */
@RestController
@RequestMapping("/api/shopping-list")
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;

    /** 查询清单（按分类分组 + 已购） */
    @GetMapping
    public Result<ShoppingListVO> detail() {
        return Result.success(shoppingListService.detail(AuthContext.requireUserId()));
    }

    /** 添加食材（含「添加其他食材」） */
    @PostMapping("/items")
    public Result<ShoppingListItem> addItem(@RequestBody ShoppingListItemAddDTO dto) {
        return Result.success(shoppingListService.addItem(AuthContext.requireUserId(), dto));
    }

    /** 勾选已购 / 修改数量 */
    @PutMapping("/items/{id}")
    public Result<ShoppingListItem> updateItem(@PathVariable Long id,
                                               @RequestBody ShoppingListItemUpdateDTO dto) {
        return Result.success(shoppingListService.updateItem(AuthContext.requireUserId(), id, dto));
    }

    /** 删除清单项 */
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        shoppingListService.deleteItem(AuthContext.requireUserId(), id);
        return Result.success();
    }

    /** 分享清单 */
    @PostMapping("/share")
    public Result<Map<String, Object>> share() {
        return Result.success(shoppingListService.share(AuthContext.requireUserId()));
    }

    /** 导出清单（纯文本下载） */
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        String text = shoppingListService.export(AuthContext.requireUserId());
        String fileName = URLEncoder.encode("shopping-list.txt", StandardCharsets.UTF_8);
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getWriter().write(text);
    }
}
