package com.dmc.archiving.billing.service;

import com.dmc.archiving.billing.model.MeterReportStatus;
import com.dmc.archiving.billing.model.StripeMeterReport;
import com.dmc.archiving.billing.repository.StripeMeterReportRepository;
import com.dmc.archiving.usage.api.DailyUsage;
import com.dmc.archiving.usage.api.UsageApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes a tenant's billable meter quantities for a billing period from the daily
 * usage snapshots and enqueues them (idempotently) to the {@link StripeMeterReport}
 * outbox. The actual push to Stripe Billing Meters is a separate, async step
 * (deferred — needs Stripe credentials); this class makes no Stripe calls.
 *
 * <p>The hard part is storage: {@code UsageSnapshot.storageBytes} is a daily STOCK
 * (point-in-time bytes), so it must NOT be summed straight into a Stripe sum meter
 * (that over-counts ~Ndays×). Instead we integrate to GB-months — see
 * {@link #gbMonthMillis}. Premium packages are a per-day FLOW and sum directly.
 */
@Service
public class BillingMeterReportService {

    /** Stripe Billing Meter event names (must match the Meters created in Stripe). */
    public static final String METER_STORAGE_GB_MONTH = "storage_gb_month";
    public static final String METER_PREMIUM_PACKAGES = "premium_packages";

    /** Bytes per GB (decimal, matching storage-pricing convention). */
    private static final long BYTES_PER_GB = 1_000_000_000L;
    /** Storage is reported in MILLI-GB-months (×1000) so the integer quantity keeps 3 dp. */
    private static final long MILLI = 1_000L;

    private final UsageApi usageApi;
    private final StripeMeterReportRepository repository;

    public BillingMeterReportService(UsageApi usageApi, StripeMeterReportRepository repository) {
        this.usageApi = usageApi;
        this.repository = repository;
    }

    /** Billable meter quantities for a tenant over {@code [periodStart, periodEnd]} inclusive. */
    public PeriodMeterTotals computePeriodTotals(Long tenantId, LocalDate periodStart, LocalDate periodEnd) {
        List<DailyUsage> days = usageApi.snapshotsForPeriod(tenantId, periodStart, periodEnd);
        long byteDays = days.stream().mapToLong(DailyUsage::storageBytes).sum();
        long premiumPackages = days.stream().mapToLong(DailyUsage::premiumPackagesGenerated).sum();
        int daysInPeriod = (int) (ChronoUnit.DAYS.between(periodStart, periodEnd) + 1);
        return new PeriodMeterTotals(gbMonthMillis(byteDays, daysInPeriod), premiumPackages);
    }

    /**
     * Convert summed daily byte-days into MILLI-GB-months (the storage meter quantity).
     * <pre>
     *   GB-month  = average GB held over the period = (Σ daily bytes / 1e9) / daysInPeriod
     *   quantity  = round(GB-month × 1000) = round(byteDays / (1e6 × daysInPeriod))
     * </pre>
     * A full period at G GB yields exactly 1000×G; holding storage for only part of the
     * period prorates. Pure integer arithmetic (round half-up) — no floating point, and
     * no overflow for realistic data (petabyte-scale × 31 days stays well within a long).
     *
     * @param byteDays      Σ of daily point-in-time storage bytes over the period (≥ 0)
     * @param daysInPeriod  calendar days in the billing period (> 0)
     */
    public static long gbMonthMillis(long byteDays, int daysInPeriod) {
        if (daysInPeriod <= 0) {
            throw new IllegalArgumentException("daysInPeriod must be positive: " + daysInPeriod);
        }
        if (byteDays < 0) {
            throw new IllegalArgumentException("byteDays must be non-negative: " + byteDays);
        }
        long divisor = (BYTES_PER_GB / MILLI) * daysInPeriod; // 1_000_000 × days
        return (byteDays + divisor / 2) / divisor;            // round half-up, all longs
    }

    /**
     * Compute the period totals and idempotently enqueue one outbox row per meter.
     * Recomputing a period updates a still-PENDING row in place (keyed by
     * {@code tenantId:periodStart:meter}); a row already SENT to Stripe is left
     * untouched so it can never be re-billed. Returns the period's outbox rows.
     */
    @Transactional
    public List<StripeMeterReport> enqueueReports(Long tenantId, LocalDate periodStart, LocalDate periodEnd) {
        PeriodMeterTotals totals = computePeriodTotals(tenantId, periodStart, periodEnd);
        return List.of(
                upsert(tenantId, periodStart, periodEnd, METER_STORAGE_GB_MONTH, totals.storageGbMonthMillis()),
                upsert(tenantId, periodStart, periodEnd, METER_PREMIUM_PACKAGES, totals.premiumPackageCount()));
    }

    private StripeMeterReport upsert(Long tenantId, LocalDate start, LocalDate end, String meter, long quantity) {
        String key = tenantId + ":" + start + ":" + meter;
        StripeMeterReport report = repository.findByIdempotencyKey(key).orElseGet(() -> {
            StripeMeterReport r = new StripeMeterReport();
            r.setTenantId(tenantId);
            r.setMeterName(meter);
            r.setPeriodStart(start);
            r.setIdempotencyKey(key);
            r.setStatus(MeterReportStatus.PENDING);
            r.setAttempts(0);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        if (report.getStatus() == MeterReportStatus.SENT) {
            return report; // already billed — never overwrite a sent quantity
        }
        report.setQuantity(quantity);
        report.setPeriodEnd(end);
        return repository.save(report);
    }

    /** Billable meter quantities for one billing period. */
    public record PeriodMeterTotals(long storageGbMonthMillis, long premiumPackageCount) {
    }
}
