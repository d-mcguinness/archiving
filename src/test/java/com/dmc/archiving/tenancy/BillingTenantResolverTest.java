package com.dmc.archiving.tenancy;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.model.TenantMembership;
import com.dmc.archiving.tenancy.repository.TenantMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the canonical billing-tenant rule (Risk 2a): attribution follows the
 * caller's membership, never a raw client-supplied tenantId.
 */
@DataJpaTest
@Import(BillingTenantResolver.class)
class BillingTenantResolverTest {

    private static final Long ORG_A = 100L;
    private static final Long ORG_B = 200L;
    private static final Long DANA = 42L;   // member of ORG_A only
    private static final Long MULTI = 43L;  // member of ORG_A and ORG_B
    private static final Long LONER = 44L;  // member of nothing

    @Autowired private TenantMembershipRepository memberships;
    @Autowired private BillingTenantResolver resolver;

    private static AuthContext user(Long id) { return new AuthContext(id, "USER", "u" + id); }
    private static AuthContext tenantMgr(Long id) { return new AuthContext(id, "TENANT", "t" + id); }
    private static AuthContext admin() { return new AuthContext(1L, "ADMIN", "admin"); }

    @BeforeEach
    void seed() {
        memberships.save(new TenantMembership(null, ORG_A, DANA, LocalDateTime.now()));
        memberships.save(new TenantMembership(null, ORG_A, MULTI, LocalDateTime.now()));
        memberships.save(new TenantMembership(null, ORG_B, MULTI, LocalDateTime.now()));
    }

    @Test
    void rejectsAttributionToATenantTheCallerIsNotAMemberOf() {
        // Dana belongs to A; claiming B must be denied (the core DoD).
        assertThatThrownBy(() -> resolver.resolve(user(DANA), ORG_B))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not a member of tenant " + ORG_B);
    }

    @Test
    void allowsClaimedTenantWhenCallerIsAMember() {
        assertThat(resolver.resolve(user(DANA), ORG_A)).isEqualTo(ORG_A);
        assertThat(resolver.resolve(tenantMgr(MULTI), ORG_B)).isEqualTo(ORG_B);
    }

    @Test
    void defaultsToSoleMembershipWhenNoTenantClaimed() {
        assertThat(resolver.resolve(user(DANA), null)).isEqualTo(ORG_A);
    }

    @Test
    void rejectsWhenNoClaimAndMembershipIsAmbiguous() {
        assertThatThrownBy(() -> resolver.resolve(tenantMgr(MULTI), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Ambiguous tenant");
    }

    @Test
    void rejectsWhenCallerBelongsToNoTenant() {
        assertThatThrownBy(() -> resolver.resolve(user(LONER), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("belongs to no tenant");
    }

    @Test
    void adminMustNameTheTenantButMayNameAny() {
        // ADMIN bypasses membership but cannot rely on an implicit tenant.
        assertThat(resolver.resolve(admin(), ORG_B)).isEqualTo(ORG_B);
        assertThatThrownBy(() -> resolver.resolve(admin(), null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN must specify");
    }

    @Test
    void rejectsUnauthenticatedCaller() {
        assertThatThrownBy(() -> resolver.resolve(AuthContext.ANONYMOUS, ORG_A))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }
}
