package com.dmc.archiving.usage.repository;

import com.dmc.archiving.usage.model.UsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageSnapshotRepository extends JpaRepository<UsageSnapshot, Long> {

    Optional<UsageSnapshot> findByTenantIdAndPeriod(Long tenantId, LocalDate period);

    List<UsageSnapshot> findByPeriod(LocalDate period);

    List<UsageSnapshot> findByTenantIdOrderByPeriodDesc(Long tenantId);

    /** Snapshots for a tenant within [start, end] inclusive, ascending — for billing-period totals. */
    List<UsageSnapshot> findByTenantIdAndPeriodBetweenOrderByPeriodAsc(Long tenantId, LocalDate start, LocalDate end);
}
