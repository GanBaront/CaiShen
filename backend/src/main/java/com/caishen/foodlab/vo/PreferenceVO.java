package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 饮食偏好返回
 */
@Data
public class PreferenceVO {

    /** 口味偏好（多选） */
    private List<String> tastes = new ArrayList<>();

    private String dietType = "不限";

    private Integer defaultServings = 2;

    private List<String> avoidItems = new ArrayList<>();

    private List<String> allergenItems = new ArrayList<>();
}
