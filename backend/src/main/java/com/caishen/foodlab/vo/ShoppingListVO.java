package com.caishen.foodlab.vo;

import com.caishen.foodlab.entity.ShoppingListItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 购物清单返回（按分类分组 + 已购）
 */
@Data
public class ShoppingListVO {

    private Long id;

    private String name;

    /** 来源菜谱(逗号分隔) */
    private String sourceRecipes;

    /** 全部清单项 */
    private List<ShoppingListItem> items = new ArrayList<>();

    /** 待购分组 */
    private List<Group> groups = new ArrayList<>();

    /** 已购项 */
    private List<ShoppingListItem> checkedItems = new ArrayList<>();

    /** 待购数量 */
    private Integer pendingCount = 0;

    /** 已购数量 */
    private Integer checkedCount = 0;

    @Data
    public static class Group {
        private String category;
        private List<ShoppingListItem> items = new ArrayList<>();

        public Group(String category) {
            this.category = category;
        }
    }
}
