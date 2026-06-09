package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.PremiumPackageEvent;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;

/**
 * Append-only read/write access to the premium-package event ledger. Exposes
 * only {@code save} plus the two counts the meter needs; no update or delete, so
 * the ledger stays immutable and delete-proof.
 *
 * <p>Because the ledger contains only billable premium generations, a plain
 * tenant count is already the premium-only total — no standard/billable filter.
 */
public interface PremiumPackageEventRepository extends Repository<PremiumPackageEvent, Long> {

    PremiumPackageEvent save(PremiumPackageEvent event);

    /** Lifetime count of premium packages generated for the tenant (for the spend-cap guard). */
    long countByTenantId(Long tenantId);

    /**
     * Premium packages generated for the tenant within the HALF-OPEN window
     * {@code [start, end)} — {@code generatedAt >= start AND generatedAt < end}.
     * Half-open so a generation at a period boundary is billed in exactly one
     * period (no double-count). Used by the per-period billing meter.
     */
    long countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
            Long tenantId, LocalDateTime start, LocalDateTime end);
}
