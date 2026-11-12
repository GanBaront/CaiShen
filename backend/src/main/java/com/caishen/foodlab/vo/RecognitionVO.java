package com.caishen.foodlab.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片识别结果：同时支持生鲜食材图与成品菜品图
 */
@Data
public class RecognitionVO {

    /** 识别出的食材名称（用于加入食材篮，菜品图也会拆解出主要食材） */
    private List<String> ingredients = new ArrayList<>();

    /** 若图中是已做好的菜品，识别出的菜名（用于展示，不入食材篮） */
    private List<String> dishes = new ArrayList<>();

    /** 识别时使用的图片地址（仅上传即识别接口回填，便于前端回显） */
    private String imageUrl;

    /**
     * 按识别出的菜名匹配到的站内菜谱（最多 4 条）。
     * 让「拍照识菜」不止停在菜名，能直接进到做法。
     */
    private List<RecipeCardVO> matchedRecipes = new ArrayList<>();
}
