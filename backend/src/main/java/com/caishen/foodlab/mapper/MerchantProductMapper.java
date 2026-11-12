package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.MerchantProduct;
import org.apache.ibatis.annotations.Mapper;

/**
 * MerchantProduct 数据访问
 */
@Mapper
public interface MerchantProductMapper extends BaseMapper<MerchantProduct> {
}