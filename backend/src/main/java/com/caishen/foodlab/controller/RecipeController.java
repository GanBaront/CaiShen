package com.caishen.foodlab.controller;

import com.caishen.foodlab.common.Result;
import com.caishen.foodlab.dto.RecipeGenerateDTO;
import com.caishen.foodlab.dto.RecipeQueryDTO;
import com.caishen.foodlab.dto.RecipeReviewDTO;
import com.caishen.foodlab.security.AuthContext;
import com.caishen.foodlab.service.OrderService;
import com.caishen.foodlab.service.RecipeReviewService;
import com.caishen.foodlab.service.RecipeService;
import com.caishen.foodlab.vo.RecipeCardVO;
import com.caishen.foodlab.vo.RecipeDetailVO;
import com.caishen.foodlab.vo.RecipeListVO;
import com.caishen.foodlab.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 菜谱：首页生成、推荐列表、今日推荐、详情、相似、收藏、加入购物清单
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final OrderService orderService;
    private final RecipeReviewService recipeReviewService;

    /** 首页「生成菜谱」（按食材 + 偏好 + 忌口过滤） */
    @PostMapping("/generate")
    public Result<List<RecipeCardVO>> generate(@RequestBody RecipeGenerateDTO dto) {
        return Result.success(recipeService.generate(AuthContext.requireUserId(), dto));
    }

    /** 菜谱推荐列表（筛选 + 排序 + 分页） */
    @GetMapping
    public Result<RecipeListVO> list(RecipeQueryDTO query) {
        return Result.success(recipeService.list(AuthContext.getUserId(), query));
    }

    /** 今日推荐 */
    @GetMapping("/today")
    public Result<List<RecipeCardVO>> today() {
        return Result.success(recipeService.today(AuthContext.getUserId()));
    }

    /** 菜谱详情 */
    @GetMapping("/{id}")
    public Result<RecipeDetailVO> detail(@PathVariable Long id,
                                         @RequestParam(required = false) Integer servings) {
        return Result.success(recipeService.detail(AuthContext.getUserId(), id, servings));
    }

    /** 相似菜谱 */
    @GetMapping("/{id}/similar")
    public Result<List<RecipeCardVO>> similar(@PathVariable Long id) {
        return Result.success(recipeService.similar(id, AuthContext.getUserId()));
    }

    /** 收藏 */
    @PostMapping("/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        recipeService.favorite(AuthContext.requireUserId(), id);
        return Result.success();
    }

    /** 取消收藏 */
    @DeleteMapping("/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        recipeService.unfavorite(AuthContext.requireUserId(), id);
        return Result.success();
    }

    /** 加入购物清单 */
    @PostMapping("/{id}/shopping-list")
    public Result<Void> addToShoppingList(@PathVariable Long id) {
        recipeService.addToShoppingList(AuthContext.requireUserId(), id);
        return Result.success();
    }

    /** 一键下单：按菜谱还缺的食材，选最优商家直接生成订单 */
    @PostMapping("/{id}/order")
    public Result<OrderVO> order(@PathVariable Long id) {
        return Result.success(orderService.createFromRecipe(AuthContext.requireUserId(), id));
    }

    /** 菜谱评价列表（仅「已通过」，管理端审核后展示） */
    @GetMapping("/{id}/reviews")
    public Result<List<Map<String, Object>>> reviews(@PathVariable Long id) {
        return Result.success(recipeReviewService.listByRecipe(id));
    }

    /** 提交菜谱评价（进入待审核，审核通过后对所有人展示） */
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody RecipeReviewDTO dto) {
        recipeReviewService.submit(AuthContext.requireUserId(), id, dto);
        return Result.success();
    }
}
