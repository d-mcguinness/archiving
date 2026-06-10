package com.dmc.archiving.web;

import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies signup is atomic (Review-PR16): when tenant provisioning fails, the
 * just-created user is rolled back — no orphan whose unique username/email would
 * be permanently taken. Uses a real RegistrationService + user persistence with a
 * TenancyApi stubbed to fail. NOT @Transactional, so we observe the real
 * commit/rollback of RegistrationService's own transaction.
 */
@SpringBootTest
class RegisterAtomicityTest {

    @Autowired private RegistrationService registrationService;
    @Autowired private UserRepository userRepository;

    @MockBean private TenancyApi tenancyApi;

    @Test
    void tenantProvisioningFailureRollsBackTheNewUser() {
        when(tenancyApi.createTenantWithOwner(anyString(), anyLong()))
                .thenThrow(new RuntimeException("provisioning failed"));

        assertThatThrownBy(() -> registrationService.register(
                "Ada", "atomic@example.com", "atomicada", "password1", "Ada's Archive"))
                .isInstanceOf(RuntimeException.class);

        // The user insert must have rolled back with the failed transaction.
        assertThat(userRepository.findByUsername("atomicada")).isEmpty();
    }
}
