package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜谱卡片返回
 */
@Data
public class RecipeCardVO {

    private Long id;

    private String name;

    private String coverImage;

    private String cuisine;

    private String difficulty;

    private String taste;

    private Integer cookTime;

    private Integer servings;

    private String description;

    private String source;

    private String status;

    /** 匹配度(%) */
    private Integer matchRate;

    /** AI 推荐理由（首页「生成菜谱」经大模型排序时返回，结合用户食材生成） */
    private String aiReason;

    /** 是否为 AI 现场创作的菜谱（站内库无匹配时临时生成，待人工审核） */
    private Boolean aiGenerated = false;

    /** 是否已收藏 */
    private Boolean favorite = false;

    /** 缺少的必备食材 */
    private List<String> missing = new ArrayList<>();

    /** 缺少但可替代的食材 */
    private List<String> substitutable = new ArrayList<>();

    /** 已有食材数量 */
    private Integer ownedCount = 0;

    private Integer totalCount = 0;
}
