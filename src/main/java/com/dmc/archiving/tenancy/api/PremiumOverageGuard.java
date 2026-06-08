package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.PremiumPackageEvent;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.repository.PremiumPackageEventRepository;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Premium-package metering for the NOARK5/E-ARK AIP+DIP rail: the soft spend cap
 * (overage guard), event recording, and the per-period billing count — all over
 * the append-only {@link PremiumPackageEvent} ledger via
 * {@link PremiumPackageEventRepository}.
 *
 * <p>Counting the ledger (immutable generation events) rather than live
 * {@code aips}/{@code dips} rows makes the meter delete-proof: deleting a
 * package cannot lower a billed counter, nor free a slot to evade the cap.
 * Reading the ledger via the tenancy-owned repository also keeps the check
 * cycle-free across the aip/dip modules.
 *
 * <p>Included-bundle and overage-budget defaults are provisional pending the
 * COGS/pricing validation; and the cap counts a cumulative lifetime total
 * rather than a billing-period rate — noted for the pricing-period work.
 */
@Component
public class PremiumOverageGuard {

    private static final Logger log = LoggerFactory.getLogger(PremiumOverageGuard.class);

    private final PremiumPackageEventRepository eventRepository;
    private final TenantOverageBudgetRepository budgetRepository;
    private final TenancyService tenancyService;

    public PremiumOverageGuard(PremiumPackageEventRepository eventRepository,
                               TenantOverageBudgetRepository budgetRepository,
                               TenancyService tenancyService) {
        this.eventRepository = eventRepository;
        this.budgetRepository = budgetRepository;
        this.tenancyService = tenancyService;
    }

    /** True if the standard (by name) is a metered premium standard. */
    public boolean isPremiumStandard(String standardName) {
        return PremiumStandards.contains(standardName);
    }

    /**
     * Append an immutable ledger event for a billable premium package that was
     * just generated. Call from within the package-creating transaction (after
     * the row is saved) so it rolls back with the package on failure. Callers
     * must only invoke this for billable premium standards (see
     * {@link #isPremiumStandard(String)}).
     */
    public void recordPremiumPackageGenerated(Long tenantId, String standardName, PremiumPackageType type) {
        eventRepository.save(new PremiumPackageEvent(tenantId, type, standardName, LocalDateTime.now()));
    }

    /**
     * Premium packages generated for the tenant within the half-open period
     * {@code [start, end)} — the per-period billing flow. Reads the ledger, so a
     * later delete cannot retroactively lower the period's billed count.
     */
    public long countGeneratedInPeriod(Long tenantId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                tenantId, start, end);
    }

    /**
     * Check whether the tenant may create one more billable premium package.
     * Throws {@link OverageSpendCapException} if it would exceed the included
     * bundle on a hard-stop plan, or exceed the overage spend cap without opt-in.
     *
     * <p>Must be called from within the package-creating transaction (it locks
     * the tenant row so the count and the subsequent insert are serialized per
     * tenant — concurrent premium creates cannot both pass the cap).
     */
    public void checkCanCreatePremiumPackage(Long tenantId) {
        Tenant tenant = tenancyService.getTenantById(tenantId);
        TenantPlan plan = tenant != null ? tenant.getPlan() : null;

        long included = includedBundle(plan);
        if (included < 0) {
            return; // unlimited (ENTERPRISE)
        }

        // Serialize concurrent premium creates for this tenant before counting.
        tenancyService.lockTenantForUpdate(tenantId);

        long current = eventRepository.countByTenantId(tenantId);
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
