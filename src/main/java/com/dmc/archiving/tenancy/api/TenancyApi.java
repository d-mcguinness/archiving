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

    /** Max number of archives for the tenant's plan; -1 means unlimited. */
    int getArchiveLimit(Long tenantId);

    /**
     * Soft spend cap: max billable storage overage beyond the plan allotment, in
     * bytes; -1 means unlimited. Uses the tenant's configured budget if present,
     * otherwise a plan default.
     */
    long getStorageOverageLimitBytes(Long tenantId);

    /** Whether the tenant has opted in to keep accruing past their spend cap. */
    boolean isOverageOptIn(Long tenantId);

    /**
     * Acquire a pessimistic per-tenant lock for the duration of the caller's
     * transaction. Call before reading usage in a quota check so the read and
     * the subsequent write are serialized per tenant (closes check-then-act
     * races). Must be invoked from within an active (@Transactional) write.
     */
    void lockTenantForUpdate(Long tenantId);

    /**
     * Max size of a single uploaded file in bytes for the tenant's plan.
     * Standard plans get the default; ENTERPRISE/CUSTOM get the raised
     * large-file ceiling (single-PUT limit).
     */
    long getMaxUploadFileSizeBytes(Long tenantId);

    /**
     * Whether the tenant's plan permits quota overage (paid plans) rather than a
     * hard stop at the allotment (FREE). Applies to storage, archives and seats.
     */
    boolean isOverageAllowed(Long tenantId);

    /** @deprecated use {@link #isOverageAllowed(Long)} — kept for the storage call site. */
    @Deprecated
    default boolean isStorageOverageAllowed(Long tenantId) {
        return isOverageAllowed(tenantId);
    }
}
