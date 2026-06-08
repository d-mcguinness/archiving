package com.dmc.archiving.aip.repository;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.archive.model.ArchiveStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AipRepository extends JpaRepository<Aip, Long> {

    List<Aip> findByTenantId(Long tenantId);

    List<Aip> findByOwnerId(Long ownerId);

    // SQL aggregate: count billable AIPs for a tenant whose standard is in the
    // given set (premium-standard metering). Excludes ADMIN-created rows.
    // Cumulative (lifetime) — used by the lifetime spend-cap guard.
    long countByTenantIdAndStandardInAndBillableTrue(Long tenantId, Collection<ArchiveStandard> standards);

    // Per-period count: billable premium AIPs GENERATED within the HALF-OPEN
    // window [start, end) — createdAt >= start AND createdAt < end. Half-open so
    // a package at a period boundary is billed in exactly one period (no
    // double-count). Used by the billing meter so a one-time generation is
    // billed once, not re-billed every period.
    long countByTenantIdAndStandardInAndBillableTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long tenantId, Collection<ArchiveStandard> standards, LocalDateTime start, LocalDateTime end);
}
