package com.caishen.foodlab.task;

import com.caishen.foodlab.service.FreeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日 0 点重置免费次数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyQuotaResetTask {

    private final FreeQuotaService freeQuotaService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyQuota() {
        freeQuotaService.resetDaily();
        log.info("每日 0 点已重置免费次数");
    }
}
