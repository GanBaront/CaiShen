package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.RecipeStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * RecipeStep 数据访问
 */
@Mapper
public interface RecipeStepMapper extends BaseMapper<RecipeStep> {
}