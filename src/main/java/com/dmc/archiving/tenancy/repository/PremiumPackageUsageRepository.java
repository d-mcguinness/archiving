package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.TenantOverageBudget;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * Read-model for the combined premium-package count (billable NOARK5/E-ARK
 * AIP + DIP) used by the live overage guard.
 *
 * <p>Deliberately queries the {@code aips}/{@code dips} TABLES via native SQL
 * rather than the Aip/Dip JPA entities. This keeps the tenancy module free of a
 * compile dependency on the aip/dip modules, which would otherwise create a
 * module cycle (aip/dip already depend on tenancy.api for the guard). The trade
 * is a real-but-Modulith-invisible coupling to those table/column names —
 * documented here so a rename is caught.
 */
public interface PremiumPackageUsageRepository extends Repository<TenantOverageBudget, Long> {

    @Query(value = "SELECT COUNT(*) FROM aips WHERE tenant_id = :tenantId "
            + "AND billable = true AND standard IN (:standards)", nativeQuery = true)
    long countBillablePremiumAips(@Param("tenantId") Long tenantId,
                                  @Param("standards") Collection<String> standards);

    @Query(value = "SELECT COUNT(*) FROM dips WHERE tenant_id = :tenantId "
            + "AND billable = true AND standard IN (:standards)", nativeQuery = true)
    long countBillablePremiumDips(@Param("tenantId") Long tenantId,
                                  @Param("standards") Collection<String> standards);
}
