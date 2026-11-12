package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.RecipeIngredient;
import org.apache.ibatis.annotations.Mapper;

/**
 * RecipeIngredient 数据访问
 */
@Mapper
public interface RecipeIngredientMapper extends BaseMapper<RecipeIngredient> {
}