package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Ingredient;
import org.apache.ibatis.annotations.Mapper;

/**
 * Ingredient 数据访问
 */
@Mapper
public interface IngredientMapper extends BaseMapper<Ingredient> {
}