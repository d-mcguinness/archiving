package com.dmc.archiving.web;

import com.dmc.archiving.auth.api.TokenSigner;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies GET /api/auth/verify actually checks the token signature (Review M1):
 * a validly-signed token is reported valid; a forged/garbage/missing token is
 * rejected with 401 rather than affirmatively reported as valid.
 */
class AuthControllerVerifyTest {

    private final TokenSigner signer = new TokenSigner("test-secret");
    private final AuthController controller = new AuthController(mock(TenancyApi.class), signer);

    @Test
    void validSignedTokenIsReportedValid() {
        ResponseEntity<?> resp = controller.verifyToken(signer.issue("tenant", "TENANT"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void forgedUnsignedTokenIsRejected() {
        // The old scheme would have accepted any non-empty header.
        ResponseEntity<?> resp = controller.verifyToken("Bearer_attacker_ADMIN_0");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(controller.verifyToken("not-a-token").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingTokenIsRejected() {
        assertThat(controller.verifyToken(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(controller.verifyToken("  ").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
