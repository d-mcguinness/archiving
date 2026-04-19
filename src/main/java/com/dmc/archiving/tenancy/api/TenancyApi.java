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
}
