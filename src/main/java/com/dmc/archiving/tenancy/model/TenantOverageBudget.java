package com.dmc.archiving.tenancy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-tenant soft spend cap on metered overage. Optional: when absent, the
 * tenant's plan defaults apply. Lets a tenant cap how far past their included
 * allotment they are willing to be billed, and opt in to keep accruing past it.
 */
@Entity
@Table(name = "tenant_overage_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantOverageBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Long tenantId;

    /** Max billable storage overage beyond the plan allotment, in bytes; -1 = unlimited. */
    @Column(name = "storage_overage_limit_bytes", nullable = false)
    private long storageOverageLimitBytes;

    /** Max billable premium-package overage beyond the included bundle; -1 = unlimited. */
    @Column(name = "premium_package_overage_limit", nullable = false)
    private long premiumPackageOverageLimit;

    /** When true, consumption may keep accruing (and billing) past the cap instead of hard-stopping. */
    @Column(name = "overage_opt_in", nullable = false)
    private boolean overageOptIn;
}