package com.caishen.foodlab.dto;

import lombok.Data;

import java.util.List;

/**
 * 我的食材篮新增入参
 * 文字输入 / 上传图片识别结果 / 语音识别结果 统一走该入口
 */
@Data
public class UserIngredientAddDTO {

    private String name;

    /** 批量录入（图片 / 语音识别多结果时使用） */
    private List<String> names;

    private String quantity;

    private String unit;

    private Long ingredientId;
}
