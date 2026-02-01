package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantStatus;

/**
 * Public API for the Tenancy module.
 * This interface defines what operations are exposed to other modules.
 */
public interface TenancyApi {

    /**
     * Check if a tenant exists and is active.
     * @param tenantId the tenant ID to check
     * @return true if tenant exists and is active, false otherwise
     */
    boolean isTenantActive(Long tenantId);

    /**
     * Get tenant by ID (for other modules to validate tenant context).
     * @param tenantId the tenant ID
     * @return Tenant object or null if not found
     */
    Tenant getTenantById(Long tenantId);

    /**
     * Check if a user belongs to a specific tenant.
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @return true if user belongs to tenant, false otherwise
     */
    boolean isUserInTenant(Long userId, Long tenantId);


}
