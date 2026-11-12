package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.RecipeHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * RecipeHistory 数据访问
 */
@Mapper
public interface RecipeHistoryMapper extends BaseMapper<RecipeHistory> {
}