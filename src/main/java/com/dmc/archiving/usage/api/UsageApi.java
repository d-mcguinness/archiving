package com.dmc.archiving.usage.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Public read API over per-tenant usage snapshots, for the billing module to
 * total a billing period. The only usage surface other modules may depend on.
 */
public interface UsageApi {

    /**
     * Daily usage snapshots for a tenant over {@code [from, to]} (both inclusive),
     * ascending by period. Empty if the tenant has no snapshots in the range.
     */
    List<DailyUsage> snapshotsForPeriod(Long tenantId, LocalDate from, LocalDate to);
}
