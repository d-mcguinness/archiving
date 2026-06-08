package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.repository.PremiumPackageUsageRepository;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Live soft spend cap for the premium-package overage rail (NOARK5/E-ARK
 * AIP + DIP). Mirrors the storage spend cap, but the count is the COMBINED
 * billable AIP+DIP total, read via {@link PremiumPackageUsageRepository} so the
 * check stays cycle-free across the aip/dip modules.
 *
 * <p>Included-bundle and overage-budget defaults are provisional pending the
 * COGS/pricing validation; and the count is a cumulative total rather than a
 * billing-period rate — both noted for the pricing-period work.
 */
@Component
public class PremiumOverageGuard {

    private static final Logger log = LoggerFactory.getLogger(PremiumOverageGuard.class);

    /** Premium standards (stored as enum-string) whose generation is metered. */
    static final List<String> PREMIUM_STANDARDS = List.of("NOARK5", "EARK");

    private final PremiumPackageUsageRepository usageRepository;
    private final TenantOverageBudgetRepository budgetRepository;
    private final TenancyService tenancyService;

    public PremiumOverageGuard(PremiumPackageUsageRepository usageRepository,
                               TenantOverageBudgetRepository budgetRepository,
                               TenancyService tenancyService) {
        this.usageRepository = usageRepository;
        this.budgetRepository = budgetRepository;
        this.tenancyService = tenancyService;
    }

    /** True if the standard (by name) is a metered premium standard. */
    public boolean isPremiumStandard(String standardName) {
        return PREMIUM_STANDARDS.contains(standardName);
    }

    /**
     * Check whether the tenant may create one more billable premium package.
     * Throws {@link OverageSpendCapException} if it would exceed the included
     * bundle on a hard-stop plan, or exceed the overage spend cap without opt-in.
     */
    public void checkCanCreatePremiumPackage(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        TenantPlan plan = tenant != null ? tenant.getPlan() : null;

        long included = includedBundle(plan);
        if (included < 0) {
            return; // unlimited (ENTERPRISE)
        }

        long current = usageRepository.countBillablePremiumAips(tenantId, PREMIUM_STANDARDS)
                + usageRepository.countBillablePremiumDips(tenantId, PREMIUM_STANDARDS);
        long projected = current + 1;
        if (projected <= included) {
            return; // within the included bundle
        }

        boolean overageAllowed = plan != null && plan != TenantPlan.FREE;
        if (!overageAllowed) {
            throw new OverageSpendCapException(
                    "Premium-package limit reached for tenant " + tenantId + " (" + included
                            + " included). Upgrade the plan to generate more premium packages.");
        }

        long overage = projected - included;
        long budget = premiumOverageLimit(tenantId, plan);
        if (budget < 0) {
            log.warn("Tenant {} premium overage {} (unlimited cap); recording billable overage",
                    tenantId, overage);
            return;
        }
        if (overage > budget && !isOptedIn(tenantId)) {
            throw new OverageSpendCapException(
                    "Premium-package overage spend cap reached for tenant " + tenantId
                            + ": overage of " + overage + " exceeds the cap of " + budget
                            + ". Raise the cap, opt in to keep accruing, or upgrade the plan.");
        }
        alert(tenantId, overage, budget);
    }

    private void alert(Long tenantId, long overage, long budget) {
        if (overage > budget) {
            log.warn("Tenant {} ACCRUING PAST premium overage cap (opted in): {} / {}", tenantId, overage, budget);
            return;
        }
        long pct = budget == 0 ? 100 : (overage * 100 / budget);
        int band = pct >= 100 ? 100 : pct >= 80 ? 80 : pct >= 50 ? 50 : 0;
        if (band > 0) {
            log.warn("Tenant {} premium-package overage at {}% of spend cap ({} / {})",
                    tenantId, band, overage, budget);
        }
    }

    private boolean isOptedIn(Long tenantId) {
        return budgetRepository.findByTenantId(tenantId).map(b -> b.isOverageOptIn()).orElse(false);
    }

    private long premiumOverageLimit(Long tenantId, TenantPlan plan) {
        return budgetRepository.findByTenantId(tenantId)
                .map(b -> b.getPremiumPackageOverageLimit())
                .orElseGet(() -> defaultPremiumOverage(plan));
    }

    /** Provisional plan-default included premium packages; -1 = unlimited. */
    private long includedBundle(TenantPlan plan) {
        if (plan == null) {
            return 0;
        }
        return switch (plan) {
            case FREE, BASIC -> 0;       // premium is a paid (Business+) capability
            case PROFESSIONAL -> 100;
            case CUSTOM -> 100;
            case ENTERPRISE -> -1;       // unlimited
        };
    }

    /** Provisional plan-default premium overage cap; -1 = unlimited. */
    private long defaultPremiumOverage(TenantPlan plan) {
        if (plan == null) {
            return 0;
        }
        return switch (plan) {
            case FREE -> 0;
            case BASIC -> 100;
            case PROFESSIONAL, CUSTOM -> 1000;
            case ENTERPRISE -> -1;
        };
    }
}
