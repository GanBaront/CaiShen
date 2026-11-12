package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

/**
 * Recipe 数据访问
 */
@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}