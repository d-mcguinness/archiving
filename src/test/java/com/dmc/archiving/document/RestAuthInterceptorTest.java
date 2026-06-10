package com.dmc.archiving.document;

import com.dmc.archiving.auth.api.TokenSigner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies REST authentication enforcement (Review C1): no/invalid token is
 * rejected with 401 before any handler runs; a valid signed token passes and
 * stashes the AuthContext. A forged unsigned token is rejected (Review M1).
 */
class RestAuthInterceptorTest {

    private final TokenSigner signer = new TokenSigner("test-secret");
    private final RestAuthInterceptor interceptor = new RestAuthInterceptor(signer);

    @Test
    void rejectsRequestWithNoToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getMethod()).thenReturn("DELETE");
        when(req.getHeader("Authorization")).thenReturn(null);
        StringWriter body = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(body));

        boolean proceed = interceptor.preHandle(req, resp, new Object());

        assertThat(proceed).isFalse();
        verify(resp).setStatus(401);
        assertThat(body.toString()).contains("Authentication required");
    }

    @Test
    void rejectsForgedUnsignedToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getMethod()).thenReturn("DELETE");
        // The old unsigned scheme: anyone could claim ADMIN. Must now be rejected.
        when(req.getHeader("Authorization")).thenReturn("Bearer_attacker_ADMIN_0");
        StringWriter body = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(body));

        boolean proceed = interceptor.preHandle(req, resp, new Object());

        assertThat(proceed).isFalse();
        verify(resp).setStatus(401);
    }

    @Test
    void passesAndStashesContextForValidToken() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getMethod()).thenReturn("DELETE");
        when(req.getHeader("Authorization")).thenReturn(signer.issue(2L, "tenant", "TENANT"));

        boolean proceed = interceptor.preHandle(req, resp, new Object());

        assertThat(proceed).isTrue();
        verify(req).setAttribute(eq(RestAuthInterceptor.AUTH_CONTEXT), any());
    }

    @Test
    void letsCorsPreflightThrough() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        when(req.getMethod()).thenReturn("OPTIONS");

        assertThat(interceptor.preHandle(req, resp, new Object())).isTrue();
    }
}
