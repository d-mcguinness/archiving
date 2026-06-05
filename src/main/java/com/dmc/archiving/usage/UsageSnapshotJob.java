package com.dmc.archiving.usage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Nightly job that captures a usage snapshot per tenant. Cron is configurable
 * via {@code usage.snapshot.cron} (default 02:00 daily).
 */
@Component
public class UsageSnapshotJob {

    private static final Logger log = LoggerFactory.getLogger(UsageSnapshotJob.class);

    private final UsageAggregationService aggregationService;

    public UsageSnapshotJob(UsageAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Scheduled(cron = "${usage.snapshot.cron:0 0 2 * * *}")
    public void captureNightly() {
        LocalDate today = LocalDate.now();
        try {
            var snapshots = aggregationService.captureAll(today);
            log.info("Usage snapshot job complete: {} tenant snapshots for {}", snapshots.size(), today);
        } catch (Exception e) {
            log.error("Usage snapshot job failed for {}: {}", today, e.getMessage(), e);
        }
    }
}
