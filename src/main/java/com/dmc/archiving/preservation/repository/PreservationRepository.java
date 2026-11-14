package com.dmc.archiving.preservation.repository;

import com.dmc.archiving.preservation.model.Preservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreservationRepository extends JpaRepository<Preservation, Long> {

    List<Preservation> findByTenantId(Long tenantId);

    List<Preservation> findByOwnerId(Long ownerId);

    // Premium-package metering no longer counts live aip rows: each billable
    // premium generation is recorded to the append-only PremiumPackageEvent
    // ledger (delete-proof), so the previous standard/billable count aggregates
    // were removed.

    // Tenant of the preservation whose element tree starts at the given root element
    @Query("SELECT p.tenantId FROM Preservation p WHERE p.rootElement.id = :rootElementId")
    Optional<Long> findTenantIdByRootElementId(@Param("rootElementId") Long rootElementId);
}
