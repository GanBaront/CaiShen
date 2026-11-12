package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.ShoppingListItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * ShoppingListItem 数据访问
 */
@Mapper
public interface ShoppingListItemMapper extends BaseMapper<ShoppingListItem> {
}