package com.dmc.archiving.tenancy.service;

import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.input.UpdateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantStatus;
import org.springframework.modulith.NamedInterface;

import java.util.List;

@NamedInterface
public interface TenancyService {
    Tenant createTenant(CreateTenantInput input);
    Tenant updateTenant(UpdateTenantInput input);
    boolean isTenantActive(Long tenantId);
    Tenant getTenantById(Long tenantId);
    boolean isUserInTenant(Long userId, Long tenantId);
    boolean deleteTenant(Long id);
    List<Tenant> getAllTenants();
    List<Tenant> getTenantsByStatus(TenantStatus status);
    List<Tenant> getTenantsByOwner(Long ownerId);
    List<Tenant> getTenantsByUserId(Long userId);  // Get all tenants a user belongs to
    List<Long> getTenantIdsByUserId(Long userId);  // Get tenant IDs for a user (exposed for other modules)
    void addUserToTenant(Long userId, Long tenantId);
    void removeUserFromTenant(Long userId);
}

