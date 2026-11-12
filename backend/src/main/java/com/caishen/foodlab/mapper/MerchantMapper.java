package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Merchant 数据访问
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}