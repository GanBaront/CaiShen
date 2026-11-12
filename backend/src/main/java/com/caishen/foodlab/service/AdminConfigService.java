package com.caishen.foodlab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.caishen.foodlab.entity.SysConfig;
import com.caishen.foodlab.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 后台系统配置：查询 / 修改（如每日免费次数、会员月费、单次费用、大模型配置）
 */
@Service
@RequiredArgsConstructor
public class AdminConfigService {

    private final SysConfigMapper sysConfigMapper;

    public List<SysConfig> list() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public List<SysConfig> update(Map<String, String> configs) {
        if (configs != null) {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                SysConfig existed = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                        .eq(SysConfig::getConfigKey, entry.getKey())
                        .last("LIMIT 1"));
                if (existed == null) {
                    SysConfig config = new SysConfig();
                    config.setConfigKey(entry.getKey());
                    config.setConfigValue(entry.getValue());
                    sysConfigMapper.insert(config);
                } else {
                    SysConfig update = new SysConfig();
                    update.setId(existed.getId());
                    update.setConfigValue(entry.getValue());
                    sysConfigMapper.updateById(update);
                }
            }
        }
        return list();
    }
}
