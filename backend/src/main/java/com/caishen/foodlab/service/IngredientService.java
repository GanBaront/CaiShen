package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.common.BusinessException;
import com.caishen.foodlab.dto.UserIngredientAddDTO;
import com.caishen.foodlab.entity.AllergenTag;
import com.caishen.foodlab.entity.Ingredient;
import com.caishen.foodlab.entity.UserIngredient;
import com.caishen.foodlab.mapper.AllergenTagMapper;
import com.caishen.foodlab.mapper.IngredientMapper;
import com.caishen.foodlab.mapper.UserIngredientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 食材库：常用食材、联想搜索、我的食材篮增删查
 * （文字输入 / 上传图片识别结果 / 语音识别结果统一走「我的食材篮」入口）
 */
@Service
@RequiredArgsConstructor
public class IngredientService {

    /** 首页「常用」快捷食材（对应设计图 洋葱 / 豆腐 / 胡萝卜 / 五花肉） */
    private static final List<String> COMMON_NAMES = List.of("洋葱", "豆腐", "胡萝卜", "五花肉");

    private final IngredientMapper ingredientMapper;
    private final UserIngredientMapper userIngredientMapper;
    private final AllergenTagMapper allergenTagMapper;

    /** 常用食材 */
    public List<Ingredient> common() {
        return ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                .in(Ingredient::getName, COMMON_NAMES)
                .eq(Ingredient::getStatus, 1)
                .orderByAsc(Ingredient::getId));
    }

    /** 食材联想/搜索（录入与「+ 添加」） */
    public List<Ingredient> search(String keyword) {
        LambdaQueryWrapper<Ingredient> wrapper = new LambdaQueryWrapper<Ingredient>()
                .eq(Ingredient::getStatus, 1)
                .orderByAsc(Ingredient::getId)
                .last("LIMIT 20");
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Ingredient::getName, kw).or().like(Ingredient::getAlias, kw));
        }
        return ingredientMapper.selectList(wrapper);
    }

    /**
     * 启用中的过敏源标签（用户端「忌口/过敏源」选择面板数据源）。
     * 管理端在过敏源标签库增删改/上下架后，这里实时反映。
     */
    public List<AllergenTag> enabledAllergenTags() {
        return allergenTagMapper.selectList(new LambdaQueryWrapper<AllergenTag>()
                .eq(AllergenTag::getStatus, 1)
                .orderByAsc(AllergenTag::getId));
    }

    /** 我的食材篮 */
    public List<UserIngredient> myIngredients(Long userId) {
        return userIngredientMapper.selectList(new LambdaQueryWrapper<UserIngredient>()
                .eq(UserIngredient::getUserId, userId)
                .orderByAsc(UserIngredient::getId));
    }

    /** 新增食材（支持单个 name 或批量 names） */
    public List<UserIngredient> add(Long userId, UserIngredientAddDTO dto) {
        List<String> names = new ArrayList<>();
        if (dto.getName() != null && !dto.getName().isBlank()) {
            // 文字输入支持顿号/逗号分隔
            for (String part : dto.getName().split("[、,，\\s]+")) {
                if (!part.isBlank()) {
                    names.add(part.trim());
                }
            }
        }
        if (dto.getNames() != null) {
            for (String name : dto.getNames()) {
                if (name != null && !name.isBlank()) {
                    names.add(name.trim());
                }
            }
        }
        if (names.isEmpty()) {
            throw new BusinessException("请输入食材名称");
        }

        List<UserIngredient> result = new ArrayList<>();
        for (String name : names) {
            Long exists = userIngredientMapper.selectCount(new LambdaQueryWrapper<UserIngredient>()
                    .eq(UserIngredient::getUserId, userId)
                    .eq(UserIngredient::getName, name));
            if (exists > 0) {
                continue;
            }
            UserIngredient item = new UserIngredient();
            item.setUserId(userId);
            item.setName(name);
            item.setQuantity(dto.getQuantity());
            item.setUnit(dto.getUnit());
            Long ingredientId = dto.getIngredientId() != null ? dto.getIngredientId() : resolveIngredientId(name);
            item.setIngredientId(ingredientId);
            userIngredientMapper.insert(item);
            result.add(item);
        }
        return result;
    }

    /** 按名称/别名匹配食材库 */
    public Long resolveIngredientId(String name) {
        List<Ingredient> list = ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                .eq(Ingredient::getName, name)
                .last("LIMIT 1"));
        if (!list.isEmpty()) {
            return list.get(0).getId();
        }
        List<Ingredient> aliasList = ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                .like(Ingredient::getAlias, name)
                .last("LIMIT 1"));
        if (!aliasList.isEmpty()) {
            String alias = aliasList.get(0).getAlias();
            if (alias != null && Arrays.stream(alias.split(",")).anyMatch(name::equals)) {
                return aliasList.get(0).getId();
            }
        }
        return null;
    }

    /** 移除食材 */
    public void remove(Long userId, Long id) {
        UserIngredient item = userIngredientMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("食材不存在");
        }
        userIngredientMapper.deleteById(id);
    }
}
