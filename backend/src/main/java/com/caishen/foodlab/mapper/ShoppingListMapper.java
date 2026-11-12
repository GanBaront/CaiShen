package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.ShoppingList;
import org.apache.ibatis.annotations.Mapper;

/**
 * ShoppingList 数据访问
 */
@Mapper
public interface ShoppingListMapper extends BaseMapper<ShoppingList> {
}