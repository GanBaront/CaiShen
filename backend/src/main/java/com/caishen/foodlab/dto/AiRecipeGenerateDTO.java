package com.caishen.foodlab.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 新菜谱生成入参（会员 / 单次体验）
 */
@Data
public class AiRecipeGenerateDTO {

    private List<String> ingredients;

    /** 来源：首页生成 / 会员AI新菜谱，默认会员AI新菜谱 */
    private String source = "会员AI新菜谱";
}
