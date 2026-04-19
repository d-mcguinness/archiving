package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class TenancyApiImpl implements TenancyApi {

    private final TenancyService tenancyService;

    TenancyApiImpl(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @Override
    public boolean isTenantActive(Long tenantId) {
        return tenancyService.isTenantActive(tenantId);
    }

    @Override
    public Tenant getTenantById(Long tenantId) {
        return tenancyService.getTenantById(tenantId);
    }

    @Override
    public boolean isUserInTenant(Long userId, Long tenantId) {
        return tenancyService.isUserInTenant(userId, tenantId);
    }

    @Override
    public List<Long> getTenantIdsByUserId(Long userId) {
        return tenancyService.getTenantIdsByUserId(userId);
    }

    @Override
    public List<Tenant> getAllTenants() {
        return tenancyService.getAllTenants();
    }

    @Override
    public long countUsersInTenant(Long tenantId) {
        return tenancyService.countUsersInTenant(tenantId);
    }
}
