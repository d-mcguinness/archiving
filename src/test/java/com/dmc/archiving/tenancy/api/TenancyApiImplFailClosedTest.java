package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.model.TenantSettings;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the storage/archive limit getters FAIL CLOSED (Review fail-closed):
 * a tenant with missing settings falls back to its plan's default allotment
 * instead of unlimited, while a legitimately-unlimited ENTERPRISE plan still
 * resolves to -1. Explicit settings always win.
 */
class TenancyApiImplFailClosedTest {

    private static final long GB = 1024L * 1024 * 1024;
    private static final long MB = 1024L * 1024;
    private static final Long TENANT = 100L;

    private final TenancyService tenancyService = mock(TenancyService.class);
    private final TenantOverageBudgetRepository budgetRepository = mock(TenantOverageBudgetRepository.class);
    private final TenancyApiImpl api = new TenancyApiImpl(tenancyService, budgetRepository);

    private Tenant tenant(TenantPlan plan, TenantSettings settings) {
        Tenant t = new Tenant();
        t.setPlan(plan);
        t.setSettings(settings);
        return t;
    }

    @Test
    void paidPlanWithNullSettingsFallsBackToPlanDefault_notUnlimited() {
        when(tenancyService.getTenantById(TENANT)).thenReturn(tenant(TenantPlan.PROFESSIONAL, null));

        assertThat(api.getStorageLimitBytes(TENANT)).isEqualTo(10 * GB); // PROFESSIONAL default, not -1
        assertThat(api.getArchiveLimit(TENANT)).isEqualTo(1000);
    }

    @Test
    void freePlanWithNullSettingsFallsBackToFreeDefault() {
        when(tenancyService.getTenantById(TENANT)).thenReturn(tenant(TenantPlan.FREE, null));

        assertThat(api.getStorageLimitBytes(TENANT)).isEqualTo(100 * MB);
        assertThat(api.getArchiveLimit(TENANT)).isEqualTo(10);
    }

    @Test
    void enterpriseWithNullSettingsStaysUnlimited() {
        when(tenancyService.getTenantById(TENANT)).thenReturn(tenant(TenantPlan.ENTERPRISE, null));

        assertThat(api.getStorageLimitBytes(TENANT)).isEqualTo(-1L);
        assertThat(api.getArchiveLimit(TENANT)).isEqualTo(-1);
    }

    @Test
    void explicitSettingsAlwaysWin() {
        TenantSettings settings = new TenantSettings(5, 7, 12345L, false, false, "UTC", "en", null);
        when(tenancyService.getTenantById(TENANT)).thenReturn(tenant(TenantPlan.BASIC, settings));

        assertThat(api.getStorageLimitBytes(TENANT)).isEqualTo(12345L);
        assertThat(api.getArchiveLimit(TENANT)).isEqualTo(7);
    }

    @Test
    void unknownTenantStaysDefensiveUnlimited() {
        when(tenancyService.getTenantById(TENANT)).thenReturn(null);

        assertThat(api.getStorageLimitBytes(TENANT)).isEqualTo(-1L);
        assertThat(api.getArchiveLimit(TENANT)).isEqualTo(-1);
    }
}
