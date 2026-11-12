package com.caishen.foodlab.dto;

import lombok.Data;

import java.util.List;

/**
 * 首页「生成菜谱」入参
 */
@Data
public class RecipeGenerateDTO {

    private List<String> ingredients;

    private List<String> tastes;

    /** 饮食方式：不限/素食/生酮/低卡 */
    private String dietType;

    private List<String> avoidItems;

    private Integer servings;
}
