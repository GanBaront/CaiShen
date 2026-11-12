package com.caishen.foodlab.dto;

import lombok.Data;

import java.util.List;

/**
 * 饮食偏好保存入参
 */
@Data
public class PreferenceUpdateDTO {

    /** 口味偏好（多选：酸/甜/辣/咸/清淡） */
    private List<String> tastes;

    /** 饮食方式：不限/素食/生酮/低卡 */
    private String dietType;

    /** 默认用餐人数 */
    private Integer defaultServings;

    /** 忌口食材 */
    private List<String> avoidItems;

    /** 过敏源 */
    private List<String> allergenItems;
}
