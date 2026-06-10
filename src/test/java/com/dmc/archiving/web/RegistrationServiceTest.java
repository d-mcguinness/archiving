package com.dmc.archiving.web;

import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the signup orchestration: register the user as a TENANT owner then
 * provision their FREE tenant; a provisioning failure must PROPAGATE (so the
 * @Transactional rollback fires) rather than being swallowed.
 */
class RegistrationServiceTest {

    private final UserApi userApi = mock(UserApi.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final RegistrationService service = new RegistrationService(userApi, tenancyApi);

    @Test
    void registersUserAsTenantOwnerThenProvisionsTenant() {
        User u = new User();
        u.setId(42L);
        u.setUsername("ada");
        u.setRole("TENANT");
        when(userApi.register("Ada", "ada@example.com", "ada", "password1", "TENANT")).thenReturn(u);
        when(tenancyApi.createTenantWithOwner("Ada's Archive", 42L)).thenReturn(9L);

        User result = service.register("Ada", "ada@example.com", "ada", "password1", "Ada's Archive");

        assertThat(result.getId()).isEqualTo(42L);
        verify(userApi).register("Ada", "ada@example.com", "ada", "password1", "TENANT");
        verify(tenancyApi).createTenantWithOwner("Ada's Archive", 42L);
    }

    @Test
    void tenantProvisioningFailurePropagates() {
        User u = new User();
        u.setId(42L);
        when(userApi.register(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(u);
        when(tenancyApi.createTenantWithOwner(anyString(), eq(42L)))
                .thenThrow(new RuntimeException("provisioning failed"));

        assertThatThrownBy(() -> service.register("Ada", "ada@example.com", "ada", "password1", "Org"))
                .isInstanceOf(RuntimeException.class);
    }
}
