package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱推荐列表返回（含忌口过滤提示）
 */
@Data
public class RecipeListVO {

    private Long total;

    private Long page;

    private Long size;

    private Long pages;

    private List<RecipeCardVO> list = new ArrayList<>();

    /** 因忌口/过敏源被隐藏的菜谱数量 */
    private Integer hiddenCount = 0;

    /** 被隐藏时命中的忌口食材 */
    private List<String> hiddenBy = new ArrayList<>();
}
