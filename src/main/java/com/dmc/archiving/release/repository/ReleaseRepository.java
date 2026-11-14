package com.dmc.archiving.release.repository;

import com.dmc.archiving.release.model.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseRepository extends JpaRepository<Release, Long> {

    List<Release> findByTenantId(Long tenantId);

    List<Release> findByOwnerId(Long ownerId);

    // Premium-package metering no longer counts live dip rows: each billable
    // premium generation is recorded to the append-only PremiumPackageEvent
    // ledger (delete-proof), so the previous standard/billable count aggregates
    // were removed.

    // Tenant of the release whose element tree starts at the given root element
    @Query("SELECT r.tenantId FROM Release r WHERE r.rootElement.id = :rootElementId")
    Optional<Long> findTenantIdByRootElementId(@Param("rootElementId") Long rootElementId);
}
