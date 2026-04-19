package com.dmc.archiving.auth;

import com.dmc.archiving.auth.api.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class GraphQlAuthInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GraphQlAuthInterceptor.class);

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String authorization = request.getHeaders().getFirst("Authorization");
        AuthContext authContext = parseToken(authorization);

        request.configureExecutionInput((executionInput, builder) ->
            builder.graphQLContext(ctx -> ctx.of("authContext", authContext)).build()
        );

        return chain.next(request);
    }

    private AuthContext parseToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return AuthContext.ANONYMOUS;
        }

        try {
            // Token format: Bearer_{username}_{role}_{UUID}
            String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;

            String[] parts = token.split("_", 4);
            if (parts.length < 4 || !"Bearer".equals(parts[0])) {
                log.warn("Invalid token format");
                return AuthContext.ANONYMOUS;
            }

            String username = parts[1];
            String role = parts[2];
            Long userId = mapUserId(username);

            return new AuthContext(userId, role, username);
        } catch (Exception e) {
            log.warn("Failed to parse auth token: {}", e.getMessage());
            return AuthContext.ANONYMOUS;
        }
    }

    private Long mapUserId(String username) {
        return switch (username.toLowerCase()) {
            case "admin" -> 1L;
            case "tenant" -> 2L;
            case "user" -> 3L;
            default -> 999L;
        };
    }
}
