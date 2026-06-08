package com.dmc.archiving.dip.repository;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.dip.model.Dip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DipRepository extends JpaRepository<Dip, Long> {

    List<Dip> findByTenantId(Long tenantId);

    List<Dip> findByOwnerId(Long ownerId);

    // SQL aggregate: count billable DIPs for a tenant whose standard is in the
    // given set (premium-standard metering). Excludes ADMIN-created rows.
    // Cumulative (lifetime) — used by the lifetime spend-cap guard.
    long countByTenantIdAndStandardInAndBillableTrue(Long tenantId, Collection<ArchiveStandard> standards);

    // Per-period count: billable premium DIPs GENERATED within the HALF-OPEN
    // window [start, end) — createdAt >= start AND createdAt < end.
    long countByTenantIdAndStandardInAndBillableTrueAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long tenantId, Collection<ArchiveStandard> standards, LocalDateTime start, LocalDateTime end);
}
