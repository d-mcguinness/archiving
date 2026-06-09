package com.dmc.archiving.usage;

import com.dmc.archiving.aip.AipService;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.dip.DipService;
import com.dmc.archiving.document.DocumentService;
import com.dmc.archiving.tenancy.api.PremiumStandards;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.usage.model.UsageSnapshot;
import com.dmc.archiving.usage.repository.UsageSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Computes per-tenant usage from SQL aggregates (never in-memory collection
 * sizes) and persists one {@link UsageSnapshot} per tenant per period. The
 * billing layer reads snapshots; it never re-counts live entities.
 */
@Service
public class UsageAggregationService {

    private static final Logger log = LoggerFactory.getLogger(UsageAggregationService.class);

    /**
     * Premium standards whose AIP/DIP generation is metered, derived (enum-typed,
     * for the JPA aggregate queries) from the canonical billing-side name set in
     * {@link PremiumStandards} — the single source of truth.
     */
    static final Set<ArchiveStandard> PREMIUM_STANDARDS =
            PremiumStandards.NAMES.stream()
                    .map(ArchiveStandard::valueOf)
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final TenancyApi tenancyApi;
    private final DocumentService documentService;
    private final AipService aipService;
    private final DipService dipService;
    private final UsageSnapshotRepository snapshotRepository;

    public UsageAggregationService(TenancyApi tenancyApi,
                                   DocumentService documentService,
                                   AipService aipService,
                                   DipService dipService,
                                   UsageSnapshotRepository snapshotRepository) {
        this.tenancyApi = tenancyApi;
        this.documentService = documentService;
        this.aipService = aipService;
        this.dipService = dipService;
        this.snapshotRepository = snapshotRepository;
    }

    /** Capture a snapshot for every tenant for the given period. */
    @Transactional
    public List<UsageSnapshot> captureAll(LocalDate period) {
        List<Tenant> tenants = tenancyApi.getAllTenants();
        log.info("Capturing usage snapshots for {} tenants for period {}", tenants.size(), period);
        return tenants.stream()
                .map(t -> capture(t.getId(), period))
                .toList();
    }

    /** Capture (insert or update) the snapshot for one tenant + period. Idempotent. */
    @Transactional
    public UsageSnapshot capture(Long tenantId, LocalDate period) {
        long storageBytes = documentService.getStorageBytesByTenant(tenantId);

        // Premium packages are a one-time generation event, so meter the FLOW
        // for this period (packages generated within the day) rather than the
        // cumulative lifetime total — otherwise $0.40 x snapshot would re-bill
        // every package ever generated, every period.
        LocalDateTime periodStart = period.atStartOfDay();
        LocalDateTime periodEnd = period.plusDays(1).atStartOfDay();
        long premiumGenerated =
                aipService.countByTenantAndStandardsGeneratedIn(tenantId, PREMIUM_STANDARDS, periodStart, periodEnd)
                        + dipService.countByTenantAndStandardsGeneratedIn(tenantId, PREMIUM_STANDARDS, periodStart, periodEnd);
        long seats = tenancyApi.countUsersInTenant(tenantId);

        UsageSnapshot snapshot = snapshotRepository
                .findByTenantIdAndPeriod(tenantId, period)
                .orElseGet(UsageSnapshot::new);

        snapshot.setTenantId(tenantId);
        snapshot.setPeriod(period);
        snapshot.setStorageBytes(storageBytes);
        snapshot.setPremiumPackagesGenerated(premiumGenerated);
        snapshot.setSeatCount(seats);
        snapshot.setCapturedAt(LocalDateTime.now());

        return snapshotRepository.save(snapshot);
    }
}
