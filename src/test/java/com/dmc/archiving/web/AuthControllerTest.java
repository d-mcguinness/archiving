package com.dmc.archiving.web;

import com.dmc.archiving.auth.LoginRequest;
import com.dmc.archiving.auth.api.TokenSigner;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies DB-backed login and self-service signup (Feature): login delegates to
 * UserApi.authenticate and issues a token carrying the real userId; bad creds are
 * 401; register creates the user + a FREE tenant they own and logs them in;
 * duplicate registration is a 400 and never provisions a tenant.
 */
class AuthControllerTest {

    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final UserApi userApi = mock(UserApi.class);
    private final TokenSigner signer = new TokenSigner("test-secret");
    private final AuthController controller = new AuthController(tenancyApi, userApi, signer);

    private static User user(Long id, String username, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(username);
        u.setEmail(username + "@example.com");
        u.setRole(role);
        return u;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<?> resp) {
        return (Map<String, Object>) resp.getBody();
    }

    @Test
    void loginWithValidCredentialsIssuesTokenAndTenant() {
        when(userApi.authenticate("tenant", "tenant123")).thenReturn(Optional.of(user(2L, "tenant", "TENANT")));
        when(tenancyApi.getTenantIdsByUserId(2L)).thenReturn(List.of(5L));

        ResponseEntity<?> resp = controller.login(new LoginRequest("tenant", "tenant123"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> b = body(resp);
        assertThat(b.get("success")).isEqualTo(true);
        assertThat(b.get("token")).isNotNull();
        assertThat(b.get("tenantId")).isEqualTo(5L);
        // token must carry the real userId
        assertThat(signer.verify((String) b.get("token")).userId()).isEqualTo(2L);
    }

    @Test
    void loginWithBadCredentialsIsUnauthorized() {
        when(userApi.authenticate("tenant", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.login(new LoginRequest("tenant", "wrong"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(resp).get("success")).isEqualTo(false);
    }

    @Test
    void registerCreatesOwnerTenantAndLogsIn() {
        User created = user(42L, "ada", "TENANT");
        when(userApi.register("Ada", "ada@example.com", "ada", "password1", "TENANT")).thenReturn(created);
        when(tenancyApi.createTenantWithOwner("Ada's Archive", 42L)).thenReturn(9L);
        when(tenancyApi.getTenantIdsByUserId(42L)).thenReturn(List.of(9L));

        RegisterRequest req = new RegisterRequest();
        req.setName("Ada");
        req.setOrganization("Ada's Archive");
        req.setEmail("ada@example.com");
        req.setUsername("ada");
        req.setPassword("password1");

        ResponseEntity<?> resp = controller.register(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> b = body(resp);
        assertThat(b.get("success")).isEqualTo(true);
        assertThat(b.get("role")).isEqualTo("TENANT");
        assertThat(b.get("tenantId")).isEqualTo(9L);
        assertThat(signer.verify((String) b.get("token")).userId()).isEqualTo(42L);
        verify(userApi).register("Ada", "ada@example.com", "ada", "password1", "TENANT");
        verify(tenancyApi).createTenantWithOwner("Ada's Archive", 42L);
    }

    @Test
    void registerWithDuplicateIsBadRequestAndProvisionsNoTenant() {
        when(userApi.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Username is already taken"));

        RegisterRequest req = new RegisterRequest();
        req.setName("Ada");
        req.setOrganization("Ada's Archive");
        req.setEmail("ada@example.com");
        req.setUsername("ada");
        req.setPassword("password1");

        ResponseEntity<?> resp = controller.register(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(resp).get("error")).isEqualTo("Username is already taken");
        verify(tenancyApi, never()).createTenantWithOwner(anyString(), anyLong());
    }
}
