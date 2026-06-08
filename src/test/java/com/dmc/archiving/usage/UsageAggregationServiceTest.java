package com.dmc.archiving.usage;

import com.dmc.archiving.aip.AipService;
import com.dmc.archiving.dip.DipService;
import com.dmc.archiving.document.DocumentService;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.usage.model.UsageSnapshot;
import com.dmc.archiving.usage.repository.UsageSnapshotRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the usage aggregation composition (Risk 1d / 3b-meter): one snapshot
 * per tenant per period; premium is a per-period FLOW (packages generated in
 * the day), not a cumulative total; idempotent upsert per period.
 */
class UsageAggregationServiceTest {

    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final AipService aipService = mock(AipService.class);
    private final DipService dipService = mock(DipService.class);
    private final UsageSnapshotRepository repo = mock(UsageSnapshotRepository.class);

    private final UsageAggregationService service =
            new UsageAggregationService(tenancyApi, documentService, aipService, dipService, repo);

    private static Tenant tenant(Long id) {
        Tenant t = new Tenant();
        t.setId(id);
        return t;
    }

    @Test
    void captureAll_writesOneSnapshotPerTenant_withPerPeriodValues() {
        LocalDate period = LocalDate.of(2026, 6, 5);
        when(tenancyApi.getAllTenants()).thenReturn(List.of(tenant(1L), tenant(2L)));

        // Tenant 1: 1000 stored bytes; 3 premium AIPs + 2 premium DIPs generated
        // this period = 5; 4 seats.
        when(documentService.getStorageBytesByTenant(1L)).thenReturn(1000L);
        when(aipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(3L);
        when(dipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(2L);
        when(tenancyApi.countUsersInTenant(1L)).thenReturn(4L);

        // Tenant 2: empty.
        when(documentService.getStorageBytesByTenant(2L)).thenReturn(0L);
        when(aipService.countByTenantAndStandardsGeneratedIn(eq(2L), any(), any(), any())).thenReturn(0L);
        when(dipService.countByTenantAndStandardsGeneratedIn(eq(2L), any(), any(), any())).thenReturn(0L);
        when(tenancyApi.countUsersInTenant(2L)).thenReturn(1L);

        when(repo.findByTenantIdAndPeriod(anyLong(), eq(period))).thenReturn(Optional.empty());
        when(repo.save(any(UsageSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        List<UsageSnapshot> snapshots = service.captureAll(period);

        assertThat(snapshots).hasSize(2);
        verify(repo, times(2)).save(any(UsageSnapshot.class));

        UsageSnapshot t1 = snapshots.stream().filter(s -> s.getTenantId() == 1L).findFirst().orElseThrow();
        assertThat(t1.getPeriod()).isEqualTo(period);
        assertThat(t1.getStorageBytes()).isEqualTo(1000L);
        assertThat(t1.getPremiumPackagesGenerated()).isEqualTo(5L); // 3 AIP + 2 DIP this period
        assertThat(t1.getSeatCount()).isEqualTo(4L);
        assertThat(t1.getCapturedAt()).isNotNull();
    }

    @Test
    void premiumIsZeroWhenNothingGeneratedThisPeriod_evenWithLifetimePackages() {
        // The DoD: a tenant with no NEW premium generations in the period is
        // billed $0 for premium that period, regardless of lifetime history.
        LocalDate period = LocalDate.of(2026, 6, 5);
        when(documentService.getStorageBytesByTenant(1L)).thenReturn(0L);
        when(aipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(0L);
        when(dipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(0L);
        when(tenancyApi.countUsersInTenant(1L)).thenReturn(3L);
        when(repo.findByTenantIdAndPeriod(1L, period)).thenReturn(Optional.empty());
        when(repo.save(any(UsageSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        UsageSnapshot snapshot = service.capture(1L, period);

        assertThat(snapshot.getPremiumPackagesGenerated()).isZero();
        // The cumulative (lifetime) count method must NOT drive the billing meter.
        verify(aipService, times(0)).countByTenantAndStandards(anyLong(), any());
        verify(dipService, times(0)).countByTenantAndStandards(anyLong(), any());
    }

    @Test
    void capture_isIdempotent_updatesExistingRowForSamePeriod() {
        LocalDate period = LocalDate.of(2026, 6, 5);
        UsageSnapshot existing = new UsageSnapshot();
        existing.setId(99L);
        existing.setTenantId(1L);
        existing.setPeriod(period);
        existing.setStorageBytes(500L); // stale value to be overwritten

        when(documentService.getStorageBytesByTenant(1L)).thenReturn(2048L);
        when(aipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(1L);
        when(dipService.countByTenantAndStandardsGeneratedIn(eq(1L), any(), any(), any())).thenReturn(0L);
        when(tenancyApi.countUsersInTenant(1L)).thenReturn(7L);
        when(repo.findByTenantIdAndPeriod(1L, period)).thenReturn(Optional.of(existing));
        when(repo.save(any(UsageSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        UsageSnapshot result = service.capture(1L, period);

        // Same row (id preserved), fresh values.
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getStorageBytes()).isEqualTo(2048L);
        assertThat(result.getPremiumPackagesGenerated()).isEqualTo(1L);
        assertThat(result.getSeatCount()).isEqualTo(7L);
    }
}
