package com.dmc.archiving.document;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.TokenSigner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Authenticates REST file/document endpoints. Parses the bearer token into an
 * {@link AuthContext} and rejects unauthenticated requests with 401 before any
 * handler runs, so no handler under the configured paths can ship unguarded.
 * The resolved context is stashed as a request attribute for handlers to read;
 * per-resource tenant-ownership (authorization) is enforced in the handlers.
 */
@Component
public class RestAuthInterceptor implements HandlerInterceptor {

    public static final String AUTH_CONTEXT = "authContext";

    private final TokenSigner tokenSigner;

    public RestAuthInterceptor(TokenSigner tokenSigner) {
        this.tokenSigner = tokenSigner;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Let CORS preflight through untouched.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        AuthContext ctx = tokenSigner.verify(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (!ctx.isAuthenticated()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"success\":false,\"error\":\"Authentication required\"}");
            return false;
        }
        request.setAttribute(AUTH_CONTEXT, ctx);
        return true;
    }
}
