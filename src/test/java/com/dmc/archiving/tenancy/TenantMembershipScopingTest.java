package com.dmc.archiving.tenancy;

import com.dmc.archiving.tenancy.model.TenantMembership;
import com.dmc.archiving.tenancy.repository.TenantMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies tenant-scoped membership removal (Risk 2b): removing a user from one
 * tenant must NOT touch their membership in another tenant — billing seat counts
 * per tenant depend on this. Backs the repository derived-delete that
 * TenancyServiceImpl.removeUserFromTenant(tenantId, userId) delegates to.
 */
@DataJpaTest
class TenantMembershipScopingTest {

    private static final Long ORG_A = 100L;
    private static final Long ORG_B = 200L;
    private static final Long DANA = 42L;
    private static final Long OTHER = 7L;

    @Autowired
    private TenantMembershipRepository repo;

    @BeforeEach
    void seed() {
        // Dana belongs to both orgs; another user also in org B.
        repo.save(new TenantMembership(null, ORG_A, DANA, LocalDateTime.now()));
        repo.save(new TenantMembership(null, ORG_B, DANA, LocalDateTime.now()));
        repo.save(new TenantMembership(null, ORG_B, OTHER, LocalDateTime.now()));
    }

    @Test
    void removingFromOneTenantLeavesOtherMembershipsIntact() {
        repo.deleteByTenantIdAndUserId(ORG_B, DANA);

        // Dana keeps her org-A membership...
        assertThat(repo.existsByTenantIdAndUserId(ORG_A, DANA)).isTrue();
        // ...and is gone from org B...
        assertThat(repo.existsByTenantIdAndUserId(ORG_B, DANA)).isFalse();
        // ...while org A's seat count is unaffected.
        assertThat(repo.countByTenantId(ORG_A)).isEqualTo(1);
        // ...and the other org-B member is untouched.
        assertThat(repo.existsByTenantIdAndUserId(ORG_B, OTHER)).isTrue();
        assertThat(repo.countByTenantId(ORG_B)).isEqualTo(1);
    }

    @Test
    void removingFromAllTenantsClearsEveryMembershipForThatUserOnly() {
        repo.deleteByUserId(DANA);

        assertThat(repo.findByUserId(DANA)).isEmpty();
        // The unrelated user's org-B membership survives.
        assertThat(repo.existsByTenantIdAndUserId(ORG_B, OTHER)).isTrue();
        assertThat(repo.countByTenantId(ORG_B)).isEqualTo(1);
    }
}
