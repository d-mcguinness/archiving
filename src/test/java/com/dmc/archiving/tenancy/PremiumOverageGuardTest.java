package com.dmc.archiving.tenancy;

import com.dmc.archiving.tenancy.api.OverageSpendCapException;
import com.dmc.archiving.tenancy.api.PremiumOverageGuard;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantOverageBudget;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.repository.PremiumPackageEventRepository;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.InOrder;

/**
 * Verifies the premium-package spend cap policy (Risk 3a-pkg): FREE/unbundled
 * plans hard-stop at the included bundle; paid plans allow overage up to the
 * cap and block past it unless opted in; ENTERPRISE is unlimited. The cap counts
 * the CURRENT billing period (per-period bundle), under a per-tenant lock taken
 * before the count.
 */
class PremiumOverageGuardTest {

    private static final Long TENANT = 100L;

    private final PremiumPackageEventRepository events = mock(PremiumPackageEventRepository.class);
    private final TenantOverageBudgetRepository budgets = mock(TenantOverageBudgetRepository.class);
    private final TenancyService tenancyService = mock(TenancyService.class);
    private final PremiumOverageGuard guard = new PremiumOverageGuard(events, budgets, tenancyService);

    private void plan(TenantPlan plan) {
        Tenant t = new Tenant();
        t.setId(TENANT);
        t.setPlan(plan);
        when(tenancyService.getTenantById(TENANT)).thenReturn(t);
    }

    /** Current per-period premium count from the append-only ledger (calendar-month window). */
    private void currentPremiumCount(long total) {
        lenient().when(events.countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                eq(TENANT), any(), any())).thenReturn(total);
    }

    private void budget(Long premiumOverageLimit, boolean optIn) {
        TenantOverageBudget b = new TenantOverageBudget();
        b.setTenantId(TENANT);
        b.setPremiumPackageOverageLimit(premiumOverageLimit);
        b.setOverageOptIn(optIn);
        when(budgets.findByTenantId(TENANT)).thenReturn(Optional.of(b));
    }

    @Test
    void enterpriseIsUnlimited_doesNotCountOrLock() {
        plan(TenantPlan.ENTERPRISE);

        assertThatCode(() -> guard.checkCanCreatePremiumPackage(TENANT)).doesNotThrowAnyException();
        verify(events, never()).countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                anyLong(), any(), any());
        verify(tenancyService, never()).lockTenantForUpdate(anyLong());
    }

    @Test
    void locksTenantBeforeCounting() {
        // The cap's race-safety depends on the per-tenant lock being taken before
        // the count (Review L6). ENTERPRISE short-circuits without locking (above).
        plan(TenantPlan.PROFESSIONAL);
        currentPremiumCount(0);

        guard.checkCanCreatePremiumPackage(TENANT);

        InOrder order = inOrder(tenancyService, events);
        order.verify(tenancyService).lockTenantForUpdate(TENANT);
        order.verify(events).countByTenantIdAndGeneratedAtGreaterThanEqualAndGeneratedAtLessThan(
                eq(TENANT), any(), any());
    }

    @Test
    void freeHardStopsAtIncludedZero() {
        plan(TenantPlan.FREE);
        currentPremiumCount(0);

        assertThatThrownBy(() -> guard.checkCanCreatePremiumPackage(TENANT))
                .isInstanceOf(OverageSpendCapException.class)
                .hasMessageContaining("Premium-package limit reached");
    }

    @Test
    void professionalWithinIncludedBundleIsAllowed() {
        plan(TenantPlan.PROFESSIONAL); // included 100
        currentPremiumCount(50);
        when(budgets.findByTenantId(TENANT)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.checkCanCreatePremiumPackage(TENANT)).doesNotThrowAnyException();
    }

    @Test
    void professionalWithinOverageBudgetIsAllowed() {
        plan(TenantPlan.PROFESSIONAL); // included 100, default premium overage 1000
        currentPremiumCount(900);  // current 900 -> projected 901, overage 801 <= 1000
        when(budgets.findByTenantId(TENANT)).thenReturn(Optional.empty());

        assertThatCode(() -> guard.checkCanCreatePremiumPackage(TENANT)).doesNotThrowAnyException();
    }

    @Test
    void professionalOverBudgetWithoutOptInIsBlocked() {
        plan(TenantPlan.PROFESSIONAL);
        currentPremiumCount(1100); // projected 1101, overage 1001 > 1000 default
        when(budgets.findByTenantId(TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.checkCanCreatePremiumPackage(TENANT))
                .isInstanceOf(OverageSpendCapException.class)
                .hasMessageContaining("overage spend cap reached");
    }

    @Test
    void overBudgetWithOptInIsAllowed() {
        plan(TenantPlan.PROFESSIONAL);
        currentPremiumCount(1100);
        budget(1000L, true); // opted in to accrue past the cap

        assertThatCode(() -> guard.checkCanCreatePremiumPackage(TENANT)).doesNotThrowAnyException();
    }
}
