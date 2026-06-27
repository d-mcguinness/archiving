package com.dmc.archiving.billing.service;

import com.dmc.archiving.billing.model.MeterReportStatus;
import com.dmc.archiving.billing.model.StripeMeterReport;
import com.dmc.archiving.billing.repository.StripeMeterReportRepository;
import com.dmc.archiving.usage.api.DailyUsage;
import com.dmc.archiving.usage.api.UsageApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static com.dmc.archiving.billing.service.BillingMeterReportService.METER_PREMIUM_PACKAGES;
import static com.dmc.archiving.billing.service.BillingMeterReportService.METER_STORAGE_GB_MONTH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the meter-report enqueue: correct quantities from the snapshots, one
 * PENDING outbox row per meter, idempotent recompute (no duplicates), and that a
 * row already SENT to Stripe is never overwritten. In-memory fake repo keyed by
 * idempotency key — no database needed.
 */
class BillingMeterReportServiceTest {

    private static final long GB = 1_000_000_000L;
    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 1, 31); // 31-day period

    private final Map<String, StripeMeterReport> store = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private UsageApi usageApi;
    private StripeMeterReportRepository repo;
    private BillingMeterReportService service;

    @BeforeEach
    void setUp() {
        usageApi = mock(UsageApi.class);
        repo = mock(StripeMeterReportRepository.class);
        when(repo.findByIdempotencyKey(anyString())).thenAnswer(inv ->
                Optional.ofNullable(store.get((String) inv.getArgument(0))));
        when(repo.save(any(StripeMeterReport.class))).thenAnswer(inv -> {
            StripeMeterReport r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(idSeq.getAndIncrement());
            }
            store.put(r.getIdempotencyKey(), r);
            return r;
        });
        service = new BillingMeterReportService(usageApi, repo);
    }

    /** 31 days at 1 GB, with `premiumOnFirstTwoDays` premium packages split 2 + 3. */
    private void givenUsage(long bytesPerDay, long premiumDay1, long premiumDay2) {
        List<DailyUsage> days = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            long premium = i == 0 ? premiumDay1 : i == 1 ? premiumDay2 : 0;
            days.add(new DailyUsage(START.plusDays(i), bytesPerDay, premium, 3));
        }
        when(usageApi.snapshotsForPeriod(1L, START, END)).thenReturn(days);
    }

    @Test
    void enqueueComputesQuantitiesAndCreatesPendingRows() {
        givenUsage(GB, 2, 3); // 1 GB every day → 1.000 GB-month = 1000 milli; premium = 5

        List<StripeMeterReport> reports = service.enqueueReports(1L, START, END);

        assertThat(reports).hasSize(2);
        StripeMeterReport storage = byMeter(METER_STORAGE_GB_MONTH);
        StripeMeterReport premium = byMeter(METER_PREMIUM_PACKAGES);
        assertThat(storage.getQuantity()).isEqualTo(1000);
        assertThat(premium.getQuantity()).isEqualTo(5);
        assertThat(storage.getStatus()).isEqualTo(MeterReportStatus.PENDING);
        assertThat(premium.getStatus()).isEqualTo(MeterReportStatus.PENDING);
        assertThat(storage.getIdempotencyKey()).isEqualTo("1:2026-01-01:" + METER_STORAGE_GB_MONTH);
        assertThat(store).hasSize(2);
    }

    @Test
    void recomputingIsIdempotent_noDuplicateRows() {
        givenUsage(GB, 1, 1);
        service.enqueueReports(1L, START, END);
        // storage grew to 2 GB for the recompute
        givenUsage(2 * GB, 1, 1);
        service.enqueueReports(1L, START, END);

        assertThat(store).hasSize(2); // still one row per meter, not four
        assertThat(byMeter(METER_STORAGE_GB_MONTH).getQuantity()).isEqualTo(2000); // updated in place
    }

    @Test
    void alreadySentReportIsNotOverwritten() {
        // A storage report for this period was already pushed to Stripe.
        StripeMeterReport sent = new StripeMeterReport();
        sent.setId(99L);
        sent.setTenantId(1L);
        sent.setMeterName(METER_STORAGE_GB_MONTH);
        sent.setPeriodStart(START);
        sent.setPeriodEnd(END);
        sent.setIdempotencyKey("1:2026-01-01:" + METER_STORAGE_GB_MONTH);
        sent.setStatus(MeterReportStatus.SENT);
        sent.setQuantity(777);
        store.put(sent.getIdempotencyKey(), sent);

        givenUsage(GB, 0, 0); // would compute 1000 for storage
        service.enqueueReports(1L, START, END);

        assertThat(byMeter(METER_STORAGE_GB_MONTH).getQuantity()).isEqualTo(777); // unchanged
        assertThat(byMeter(METER_STORAGE_GB_MONTH).getStatus()).isEqualTo(MeterReportStatus.SENT);
    }

    private StripeMeterReport byMeter(String meter) {
        return store.values().stream().filter(r -> r.getMeterName().equals(meter)).findFirst().orElseThrow();
    }
}
