package com.caishen.foodlab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.caishen.foodlab.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserPreference 数据访问
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}