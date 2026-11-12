package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.RecipeFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * RecipeFavorite 数据访问
 */
@Mapper
public interface RecipeFavoriteMapper extends BaseMapper<RecipeFavorite> {
}