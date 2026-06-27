package com.dmc.archiving.usage.api;

import com.dmc.archiving.usage.repository.UsageSnapshotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Maps internal {@code UsageSnapshot} rows to the billing-facing {@link DailyUsage}
 * projection. Lives in the api package (the module's exposed surface) but reads the
 * internal repository — intra-module access is fine.
 */
@Service
public class UsageApiImpl implements UsageApi {

    private final UsageSnapshotRepository snapshotRepository;

    public UsageApiImpl(UsageSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Override
    public List<DailyUsage> snapshotsForPeriod(Long tenantId, LocalDate from, LocalDate to) {
        return snapshotRepository.findByTenantIdAndPeriodBetweenOrderByPeriodAsc(tenantId, from, to).stream()
                .map(s -> new DailyUsage(
                        s.getPeriod(), s.getStorageBytes(), s.getPremiumPackagesGenerated(), s.getSeatCount()))
                .toList();
    }
}
