package com.dmc.archiving.auth.api;

/**
 * Parses the app's bearer token ({@code Bearer_{username}_{role}_{uuid}}) into
 * an {@link AuthContext}. Shared by the GraphQL interceptor and REST endpoints
 * so both derive identity the same way.
 */
public final class AuthTokens {

    private AuthTokens() {}

    public static AuthContext parse(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return AuthContext.ANONYMOUS;
        }
        try {
            String token = authorizationHeader.startsWith("Bearer ")
                    ? authorizationHeader.substring(7)
                    : authorizationHeader;

            String[] parts = token.split("_", 4);
            if (parts.length < 4 || !"Bearer".equals(parts[0])) {
                return AuthContext.ANONYMOUS;
            }

            String username = parts[1];
            String role = parts[2];
            return new AuthContext(mapUserId(username), role, username);
        } catch (Exception e) {
            return AuthContext.ANONYMOUS;
        }
    }

    private static Long mapUserId(String username) {
        return switch (username.toLowerCase()) {
            case "admin" -> 1L;
            case "tenant" -> 2L;
            case "user" -> 3L;
            default -> 999L;
        };
    }
}
