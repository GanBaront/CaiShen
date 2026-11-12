package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 新菜谱预览返回
 */
@Data
public class AiRecipePreviewVO {

    private Long id;

    private String label = "AI 创作";

    private String name;

    private Integer cookTime;

    private String difficulty;

    private String taste;

    private String description;

    /** 食材名列表 */
    private List<String> ingredients = new ArrayList<>();

    /** 需要登录 / 会员或单次购买后可生成 */
    private Boolean needPurchase = false;

    private String tip;
}
