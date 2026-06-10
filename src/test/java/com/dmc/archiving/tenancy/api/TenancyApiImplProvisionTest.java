package com.dmc.archiving.tenancy.api;

import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.repository.TenantOverageBudgetRepository;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies self-service tenant provisioning (Feature): createTenantWithOwner
 * creates a FREE-plan tenant with a unique (owner-suffixed) domain and adds the
 * owner as a member, returning the new tenant id.
 */
class TenancyApiImplProvisionTest {

    private final TenancyService tenancyService = mock(TenancyService.class);
    private final TenantOverageBudgetRepository budgets = mock(TenantOverageBudgetRepository.class);
    private final TenancyApiImpl api = new TenancyApiImpl(tenancyService, budgets);

    @Test
    void createsFreeTenantWithUniqueDomainAndAddsOwner() {
        Tenant created = new Tenant();
        created.setId(9L);
        when(tenancyService.createTenant(any(CreateTenantInput.class))).thenReturn(created);

        Long tenantId = api.createTenantWithOwner("Ada's Archive", 42L);

        assertThat(tenantId).isEqualTo(9L);

        ArgumentCaptor<CreateTenantInput> input = ArgumentCaptor.forClass(CreateTenantInput.class);
        verify(tenancyService).createTenant(input.capture());
        assertThat(input.getValue().getPlan()).isEqualTo(TenantPlan.FREE);
        assertThat(input.getValue().getName()).isEqualTo("Ada's Archive");
        assertThat(input.getValue().getOwnerId()).isEqualTo("42");
        // owner id is suffixed for a guaranteed-unique domain
        assertThat(input.getValue().getDomain()).isEqualTo("ada-s-archive-42");

        verify(tenancyService).addUserToTenant(42L, 9L);
    }

    @Test
    void blankOrgNameFallsBackToADefault() {
        Tenant created = new Tenant();
        created.setId(3L);
        when(tenancyService.createTenant(any(CreateTenantInput.class))).thenReturn(created);

        api.createTenantWithOwner("   ", 7L);

        ArgumentCaptor<CreateTenantInput> input = ArgumentCaptor.forClass(CreateTenantInput.class);
        verify(tenancyService).createTenant(input.capture());
        assertThat(input.getValue().getName()).isEqualTo("My Organization");
        assertThat(input.getValue().getDomain()).isEqualTo("my-organization-7");
    }
}
