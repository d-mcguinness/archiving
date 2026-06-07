package com.dmc.archiving.tenancy;

import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantMembership;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.model.TenantSettings;
import com.dmc.archiving.tenancy.repository.TenancyRepository;
import com.dmc.archiving.tenancy.repository.TenantMembershipRepository;
import com.dmc.archiving.tenancy.service.TenancyServiceImpl;
import com.dmc.archiving.user.api.UserApi;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies maxUsers (seat) enforcement when adding a user to a tenant (Risk 1c):
 * FREE hard-stops at the seat allotment; paid plans allow billed overage.
 */
class SeatQuotaTest {

    private static final Long TENANT = 100L;
    private static final Long USER = 50L;

    private final TenancyRepository tenancyRepo = mock(TenancyRepository.class);
    private final TenantMembershipRepository memberships = mock(TenantMembershipRepository.class);
    private final UserApi userApi = mock(UserApi.class);
    private final TenancyServiceImpl service = new TenancyServiceImpl(tenancyRepo, memberships, userApi);

    private void commonStubs() {
        when(userApi.userExists(USER)).thenReturn(true);
        when(tenancyRepo.existsById(TENANT)).thenReturn(true);
        when(memberships.existsByTenantIdAndUserId(TENANT, USER)).thenReturn(false);
        lenient().when(memberships.save(any(TenantMembership.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Tenant tenant(TenantPlan plan, Integer maxUsers) {
        Tenant t = new Tenant();
        t.setId(TENANT);
        t.setPlan(plan);
        TenantSettings s = new TenantSettings();
        s.setMaxUsers(maxUsers);
        t.setSettings(s);
        return t;
    }

    @Test
    void freePlanHardStopsAtSeatLimit() {
        commonStubs();
        when(tenancyRepo.findByIdForUpdate(TENANT)).thenReturn(Optional.of(tenant(TenantPlan.FREE, 5)));
        when(memberships.countByTenantId(TENANT)).thenReturn(5L);

        assertThatThrownBy(() -> service.addUserToTenant(USER, TENANT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat limit reached");

        verify(memberships, never()).save(any());
    }

    @Test
    void paidPlanAllowsBilledSeatOverage() {
        commonStubs();
        when(tenancyRepo.findByIdForUpdate(TENANT)).thenReturn(Optional.of(tenant(TenantPlan.BASIC, 5)));
        when(memberships.countByTenantId(TENANT)).thenReturn(5L);

        service.addUserToTenant(USER, TENANT);

        verify(memberships).save(any(TenantMembership.class));
    }

    @Test
    void withinSeatAllotmentProceeds() {
        commonStubs();
        when(tenancyRepo.findByIdForUpdate(TENANT)).thenReturn(Optional.of(tenant(TenantPlan.FREE, 5)));
        when(memberships.countByTenantId(TENANT)).thenReturn(2L);

        service.addUserToTenant(USER, TENANT);

        verify(memberships).save(any(TenantMembership.class));
    }

    @Test
    void unlimitedSeatsProceed() {
        commonStubs();
        when(tenancyRepo.findByIdForUpdate(TENANT)).thenReturn(Optional.of(tenant(TenantPlan.ENTERPRISE, -1)));

        service.addUserToTenant(USER, TENANT);

        verify(memberships).save(any(TenantMembership.class));
        verify(memberships, never()).countByTenantId(anyLong());
    }
}
