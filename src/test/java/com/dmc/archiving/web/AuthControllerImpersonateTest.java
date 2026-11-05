package com.dmc.archiving.web;

import com.dmc.archiving.auth.RefreshTokenService;
import com.dmc.archiving.auth.api.AuthContext;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * POST /api/auth/impersonate issues a backend-signed token for the target user,
 * recording the caller as the actor, under these rules: ADMIN → any non-ADMIN;
 * TENANT → a USER who shares a tenant with them; nobody → an ADMIN, themselves,
 * or anyone while already impersonating. No refresh token is ever issued.
 */
class AuthControllerImpersonateTest {

    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final UserApi userApi = mock(UserApi.class);
    private final TokenSigner signer = new TokenSigner("test-secret");
    private final AuthController controller = new AuthController(
            tenancyApi, userApi, signer, mock(RegistrationService.class),
            new SignupRateLimiter(100, 60_000L, System::currentTimeMillis),
            new LoginAttemptLimiter(100, 60_000L, System::currentTimeMillis),
            mock(RefreshTokenService.class),
            new RefreshRateLimiter(100, 60_000L, System::currentTimeMillis));

    private static User user(long id, String username, String role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setName(username);
        u.setEmail(username + "@example.com");
        u.setRole(role);
        return u;
    }

    private final User admin = user(1L, "admin", "ADMIN");
    private final User owner = user(2L, "owner", "TENANT");
    private final User member = user(3L, "member", "USER");
    private final User outsider = user(4L, "outsider", "USER");

    private String tokenFor(User u) {
        return signer.issue(u.getId(), u.getUsername(), u.getRole());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> body(ResponseEntity<?> r) {
        return (Map<String, Object>) r.getBody();
    }

    private void seed() {
        for (User u : List.of(admin, owner, member, outsider)) {
            when(userApi.getUserById(u.getId())).thenReturn(Optional.of(u));
        }
        when(tenancyApi.getTenantIdsByUserId(owner.getId())).thenReturn(List.of(10L));
        when(tenancyApi.getTenantIdsByUserId(member.getId())).thenReturn(List.of(10L, 11L));
        when(tenancyApi.getTenantIdsByUserId(outsider.getId())).thenReturn(List.of(20L));
        when(tenancyApi.isUserInTenant(member.getId(), 10L)).thenReturn(true);
        when(tenancyApi.isUserInTenant(outsider.getId(), 10L)).thenReturn(false);
    }

    @Test
    void adminGetsASignedTokenForTheTargetWithActorAndNoRefreshToken() {
        seed();
        ResponseEntity<?> r = controller.impersonate(tokenFor(admin), Map.of("userId", "3"));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> b = body(r);
        assertThat(b).containsEntry("success", true).containsEntry("role", "USER")
                .containsEntry("tenantId", 10L).containsEntry("impersonatedBy", 1L);
        assertThat(b.get("refreshToken")).isNull();
        assertThat((Long) b.get("expiresIn")).isLessThanOrEqualTo(30 * 60);

        AuthContext ctx = signer.verify((String) b.get("token"));
        assertThat(ctx.isAuthenticated()).isTrue();
        assertThat(ctx.userId()).isEqualTo(3L);
        assertThat(ctx.role()).isEqualTo("USER");
        assertThat(ctx.isImpersonated()).isTrue();
        assertThat(ctx.actorId()).isEqualTo(1L);
    }

    @Test
    void requestedTenantMustBeOneOfTheTargetsTenants() {
        seed();
        assertThat(body(controller.impersonate(tokenFor(admin), Map.of("userId", "3", "tenantId", "11"))))
                .containsEntry("tenantId", 11L);
        ResponseEntity<?> bad = controller.impersonate(tokenFor(admin), Map.of("userId", "3", "tenantId", "99"));
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tenantOwnerMayImpersonateAMemberOfTheirTenantOnly() {
        seed();
        assertThat(controller.impersonate(tokenFor(owner), Map.of("userId", "3")).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<?> other = controller.impersonate(tokenFor(owner), Map.of("userId", "4"));
        assertThat(other.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(body(other).get("error").toString()).contains("not a member");

        ResponseEntity<?> asAdmin = controller.impersonate(tokenFor(owner), Map.of("userId", "1"));
        assertThat(asAdmin.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nobodyImpersonatesAnAdminAUserMayNotImpersonateAtAll() {
        seed();
        assertThat(controller.impersonate(tokenFor(admin), Map.of("userId", "1")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST); // self
        User admin2 = user(5L, "admin2", "ADMIN");
        when(userApi.getUserById(5L)).thenReturn(Optional.of(admin2));
        assertThat(controller.impersonate(tokenFor(admin), Map.of("userId", "5")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(controller.impersonate(tokenFor(member), Map.of("userId", "4")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anonymousMalformedAndNestedRequestsAreRejected() {
        seed();
        assertThat(controller.impersonate(null, Map.of("userId", "3")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.impersonate("Bearer_mimic_3_USER_123", Map.of("userId", "3")).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.impersonate(tokenFor(admin), Map.of()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.impersonate(tokenFor(admin), Map.of("userId", "abc")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.impersonate(tokenFor(admin), Map.of("userId", "999")).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Already impersonating: the impersonation token cannot be used to hop to a third user
        String hop = (String) body(controller.impersonate(tokenFor(admin), Map.of("userId", "3"))).get("token");
        assertThat(controller.impersonate(hop, Map.of("userId", "4")).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
