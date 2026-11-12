package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.UserIngredient;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserIngredient 数据访问
 */
@Mapper
public interface UserIngredientMapper extends BaseMapper<UserIngredient> {
}