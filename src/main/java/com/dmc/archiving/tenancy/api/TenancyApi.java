package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;

import java.util.List;

/**
 * Public API for the Tenancy module.
 * This interface defines what operations are exposed to other modules.
 */
public interface TenancyApi {

    boolean isTenantActive(Long tenantId);

    Tenant getTenantById(Long tenantId);

    boolean isUserInTenant(Long userId, Long tenantId);

    List<Long> getTenantIdsByUserId(Long userId);

    List<Tenant> getAllTenants();

    long countUsersInTenant(Long tenantId);

    /** Storage allotment for the tenant's plan in bytes; -1 means unlimited. */
    long getStorageLimitBytes(Long tenantId);

    /** Whether the tenant's plan permits storage overage (paid plans) vs. a hard stop (FREE). */
    boolean isStorageOverageAllowed(Long tenantId);
}
