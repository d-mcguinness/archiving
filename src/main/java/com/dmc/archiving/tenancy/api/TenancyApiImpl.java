package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class TenancyApiImpl implements TenancyApi {

    private static final long GB = 1024L * 1024 * 1024;
    private static final long MB = 1024L * 1024;
    /** Default per-file upload cap for standard plans. */
    private static final long DEFAULT_MAX_UPLOAD = 50 * MB;
    /** Raised large-file ceiling for ENTERPRISE/CUSTOM: the S3 single-PUT limit. */
    private static final long LARGE_FILE_MAX_UPLOAD = 5 * GB;

    private final TenancyService tenancyService;
    private final TenantOverageBudgetRepository overageBudgetRepository;

    TenancyApiImpl(TenancyService tenancyService,
                   TenantOverageBudgetRepository overageBudgetRepository) {
        this.tenancyService = tenancyService;
        this.overageBudgetRepository = overageBudgetRepository;
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

    @Override
    public long getStorageLimitBytes(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        if (tenant == null || tenant.getSettings() == null
                || tenant.getSettings().getMaxStorageBytes() == null) {
            return -1L; // unknown settings -> treat as unlimited rather than block
        }
        return tenant.getSettings().getMaxStorageBytes();
    }

    @Override
    public int getArchiveLimit(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        if (tenant == null || tenant.getSettings() == null
                || tenant.getSettings().getMaxArchives() == null) {
            return -1; // unknown settings -> treat as unlimited rather than block
        }
        return tenant.getSettings().getMaxArchives();
    }

    @Override
    public boolean isOverageAllowed(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        // FREE hard-stops at its allotment; every paid plan permits (billed) overage.
        return tenant != null && tenant.getPlan() != TenantPlan.FREE;
    }

    @Override
    public long getStorageOverageLimitBytes(Long tenantId) {
        return overageBudgetRepository.findByTenantId(tenantId)
                .map(b -> b.getStorageOverageLimitBytes())
                .orElseGet(() -> defaultStorageOverageBytes(tenantId));
    }

    @Override
    public boolean isOverageOptIn(Long tenantId) {
        return overageBudgetRepository.findByTenantId(tenantId)
                .map(b -> b.isOverageOptIn())
                .orElse(false);
    }

    @Override
    public long getMaxUploadFileSizeBytes(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        TenantPlan plan = tenant != null ? tenant.getPlan() : null;
        if (plan == TenantPlan.ENTERPRISE || plan == TenantPlan.CUSTOM) {
            return LARGE_FILE_MAX_UPLOAD;
        }
        return DEFAULT_MAX_UPLOAD;
    }

    /** Plan-default spend cap when the tenant has not configured one. */
    private long defaultStorageOverageBytes(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        TenantPlan plan = tenant != null ? tenant.getPlan() : null;
        if (plan == null) {
            return 0L;
        }
        return switch (plan) {
            case FREE -> 0L;                 // no overage (hard-stops at allotment)
            case BASIC -> GB;                // 1 GB
            case PROFESSIONAL -> 10 * GB;    // 10 GB
            case CUSTOM -> 5 * GB;           // 5 GB
            case ENTERPRISE -> -1L;          // unlimited
        };
    }
}
