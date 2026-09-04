package com.dmc.archiving.preservation.repository;

import com.dmc.archiving.preservation.model.Preservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreservationRepository extends JpaRepository<Preservation, Long> {

    List<Preservation> findByTenantId(Long tenantId);

    List<Preservation> findByOwnerId(Long ownerId);

    // Premium-package metering no longer counts live aip rows: each billable
    // premium generation is recorded to the append-only PremiumPackageEvent
    // ledger (delete-proof), so the previous standard/billable count aggregates
    // were removed.
}
