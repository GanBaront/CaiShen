package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.ShoppingListItemAddDTO;
import com.caishen.foodlab.dto.ShoppingListItemUpdateDTO;
import com.caishen.foodlab.entity.MerchantProduct;
import com.caishen.foodlab.entity.ShoppingList;
import com.caishen.foodlab.entity.ShoppingListItem;
import com.caishen.foodlab.mapper.MerchantProductMapper;
import com.caishen.foodlab.mapper.ShoppingListItemMapper;
import com.caishen.foodlab.mapper.ShoppingListMapper;
import com.caishen.foodlab.vo.ShoppingListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物清单：查询（按分类分组 + 已购）、增改删、分享、导出
 */
@Service
@RequiredArgsConstructor
public class ShoppingListService {

    private final ShoppingListMapper shoppingListMapper;
    private final ShoppingListItemMapper shoppingListItemMapper;
    private final MerchantProductMapper merchantProductMapper;

    /** 取当前用户的默认清单，不存在则创建 */
    public ShoppingList getOrCreate(Long userId) {
        List<ShoppingList> lists = shoppingListMapper.selectList(new LambdaQueryWrapper<ShoppingList>()
                .eq(ShoppingList::getUserId, userId)
                .orderByAsc(ShoppingList::getId)
                .last("LIMIT 1"));
        if (!lists.isEmpty()) {
            return lists.get(0);
        }
        ShoppingList list = new ShoppingList();
        list.setUserId(userId);
        list.setName("我的购物清单");
        shoppingListMapper.insert(list);
        return list;
    }

    /** 查询清单（按分类分组 + 已购） */
    public ShoppingListVO detail(Long userId) {
        ShoppingList list = getOrCreate(userId);
        List<ShoppingListItem> items = shoppingListItemMapper.selectList(new LambdaQueryWrapper<ShoppingListItem>()
                .eq(ShoppingListItem::getListId, list.getId())
                .orderByAsc(ShoppingListItem::getSort)
                .orderByAsc(ShoppingListItem::getId));

        ShoppingListVO vo = new ShoppingListVO();
        vo.setId(list.getId());
        vo.setName(list.getName());
        vo.setSourceRecipes(list.getSourceRecipes());
        vo.setItems(items);
        // 为每项匹配商家最低价商品的售卖规格（购买单位）与参考价
        fillSellUnit(items);

        Map<String, ShoppingListVO.Group> groups = new LinkedHashMap<>();
        List<ShoppingListItem> checked = new ArrayList<>();
        for (ShoppingListItem item : items) {
            if (item.getChecked() != null && item.getChecked() == 1) {
                checked.add(item);
                continue;
            }
            String category = item.getCategory() == null || item.getCategory().isBlank()
                    ? "其他" : item.getCategory();
            groups.computeIfAbsent(category, ShoppingListVO.Group::new).getItems().add(item);
        }
        vo.setGroups(new ArrayList<>(groups.values()));
        vo.setCheckedItems(checked);
        vo.setPendingCount(items.size() - checked.size());
        vo.setCheckedCount(checked.size());
        return vo;
    }

    /** 添加食材（含「添加其他食材」） */
    public ShoppingListItem addItem(Long userId, ShoppingListItemAddDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BusinessException("请输入食材名称");
        }
        ShoppingList list = getOrCreate(userId);
        String name = dto.getName().trim();

        // 同名食材已在清单里就不再加一行：重复行会让「下单」出现两条一样的明细，
        // 也让用户以为自己加错了。改为补全数量/单位。
        ShoppingListItem existed = shoppingListItemMapper.selectOne(new LambdaQueryWrapper<ShoppingListItem>()
                .eq(ShoppingListItem::getListId, list.getId())
                .eq(ShoppingListItem::getName, name)
                .eq(ShoppingListItem::getChecked, 0)
                .last("LIMIT 1"));
        if (existed != null) {
            boolean changed = false;
            if (dto.getAmount() != null && !dto.getAmount().isBlank()) {
                existed.setAmount(dto.getAmount());
                changed = true;
            }
            if (dto.getUnit() != null && !dto.getUnit().isBlank()) {
                existed.setUnit(dto.getUnit());
                changed = true;
            }
            if (dto.getIngredientId() != null) {
                existed.setIngredientId(dto.getIngredientId());
                changed = true;
            }
            if (changed) {
                shoppingListItemMapper.updateById(existed);
            }
            return existed;
        }

        ShoppingListItem item = new ShoppingListItem();
        item.setListId(list.getId());
        item.setIngredientId(dto.getIngredientId());
        item.setName(name);
        item.setAmount(dto.getAmount());
        item.setUnit(dto.getUnit());
        item.setCategory(dto.getCategory() == null || dto.getCategory().isBlank() ? "其他" : dto.getCategory());
        item.setChecked(0);
        item.setSort(nextSort(list.getId()));
        shoppingListItemMapper.insert(item);
        return item;
    }

    /** 加入清单项（供「加入购物清单」复用） */
    public void addItem(Long listId, String name, String amount, String unit, String category,
                         Long ingredientId, String recipeSource) {
        ShoppingListItem item = new ShoppingListItem();
        item.setListId(listId);
        item.setName(name);
        item.setAmount(amount);
        item.setUnit(unit);
        item.setCategory(category == null || category.isBlank() ? "其他" : category);
        item.setChecked(0);
        item.setIngredientId(ingredientId);
        item.setRecipeSource(recipeSource);
        item.setSort(nextSort(listId));
        shoppingListItemMapper.insert(item);
    }

    /** 记录来源菜谱 */
    public void appendSourceRecipe(ShoppingList list, String recipeName) {
        String source = list.getSourceRecipes();
        if (source == null || source.isBlank()) {
            source = recipeName;
        } else if (!List.of(source.split(",")).contains(recipeName)) {
            source = source + "," + recipeName;
        } else {
            return;
        }
        ShoppingList update = new ShoppingList();
        update.setId(list.getId());
        update.setSourceRecipes(source);
        shoppingListMapper.updateById(update);
        list.setSourceRecipes(source);
    }

    /** 勾选已购 / 修改数量 */
    public ShoppingListItem updateItem(Long userId, Long itemId, ShoppingListItemUpdateDTO dto) {
        ShoppingListItem item = requireOwn(userId, itemId);
        ShoppingListItem update = new ShoppingListItem();
        update.setId(item.getId());
        if (dto.getChecked() != null) {
            update.setChecked(dto.getChecked());
        }
        if (dto.getAmount() != null) {
            update.setAmount(dto.getAmount());
        }
        shoppingListItemMapper.updateById(update);
        return shoppingListItemMapper.selectById(itemId);
    }

    /** 删除清单项 */
    public void deleteItem(Long userId, Long itemId) {
        requireOwn(userId, itemId);
        shoppingListItemMapper.deleteById(itemId);
    }

    /** 分享清单（返回可分享文案） */
    public Map<String, Object> share(Long userId) {
        ShoppingListVO vo = detail(userId);
        StringBuilder text = new StringBuilder("【材神食验室 · 购物清单】\n");
        for (ShoppingListVO.Group group : vo.getGroups()) {
            text.append(group.getCategory()).append("：");
            for (ShoppingListItem item : group.getItems()) {
                text.append(item.getName());
                if (item.getAmount() != null) {
                    text.append(" ").append(item.getAmount());
                }
                if (item.getUnit() != null) {
                    text.append(item.getUnit());
                }
                text.append("、");
            }
            text.setLength(text.length() - 1);
            text.append("\n");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("listId", vo.getId());
        result.put("shareText", text.toString().trim());
        return result;
    }

    /** 导出清单（纯文本） */
    public String export(Long userId) {
        ShoppingListVO vo = detail(userId);
        StringBuilder text = new StringBuilder("材神食验室 购物清单\n来源菜谱：" )
                .append(vo.getSourceRecipes() == null ? "无" : vo.getSourceRecipes()).append("\n");
        text.append("—— 待购 ").append(vo.getPendingCount()).append(" 项 ——\n");
        for (ShoppingListVO.Group group : vo.getGroups()) {
            text.append("[").append(group.getCategory()).append("]\n");
            for (ShoppingListItem item : group.getItems()) {
                text.append("  - ").append(item.getName());
                if (item.getAmount() != null) {
                    text.append(" ").append(item.getAmount());
                }
                if (item.getUnit() != null) {
                    text.append(item.getUnit());
                }
                text.append("\n");
            }
        }
        if (!vo.getCheckedItems().isEmpty()) {
            text.append("—— 已购 ").append(vo.getCheckedCount()).append(" 项 ——\n");
            for (ShoppingListItem item : vo.getCheckedItems()) {
                text.append("  ✓ ").append(item.getName()).append("\n");
            }
        }
        return text.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    public void clearList(Long listId) {
        shoppingListItemMapper.delete(new LambdaQueryWrapper<ShoppingListItem>()
                .eq(ShoppingListItem::getListId, listId));
    }

    /**
     * 为清单项匹配「购买单位」：
     * 在所有已上架商品中找同名/同食材里价格最低的一家，取其售卖规格与价格。
     * 购物清单页展示购买规格，而不是菜谱里的汤匙/茶匙等烹饪用量。
     */
    private void fillSellUnit(List<ShoppingListItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<MerchantProduct> products = merchantProductMapper.selectList(
                new LambdaQueryWrapper<MerchantProduct>()
                        .eq(MerchantProduct::getAuditStatus, "已通过")
                        .eq(MerchantProduct::getStatus, 1));
        for (ShoppingListItem item : items) {
            MerchantProduct best = null;
            for (MerchantProduct product : products) {
                boolean match = (item.getIngredientId() != null && item.getIngredientId().equals(product.getIngredientId()))
                        || (item.getName() != null && item.getName().equals(product.getName()));
                if (!match) {
                    continue;
                }
                if (best == null || (product.getPrice() != null
                        && (best.getPrice() == null || product.getPrice().compareTo(best.getPrice()) < 0))) {
                    best = product;
                }
            }
            if (best != null) {
                item.setSellUnit(best.getSellUnit());
                item.setBestPrice(best.getPrice());
            }
        }
    }

    private ShoppingListItem requireOwn(Long userId, Long itemId) {
        ShoppingListItem item = shoppingListItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("清单项不存在");
        }
        ShoppingList list = shoppingListMapper.selectById(item.getListId());
        if (list == null || !list.getUserId().equals(userId)) {
            throw new BusinessException("清单项不存在");
        }
        return item;
    }

    private int nextSort(Long listId) {
        Long count = shoppingListItemMapper.selectCount(new LambdaQueryWrapper<ShoppingListItem>()
                .eq(ShoppingListItem::getListId, listId));
        return (int) (count + 1);
    }
}
