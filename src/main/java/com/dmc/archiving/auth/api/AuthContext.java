package com.dmc.archiving.auth.api;

/**
 * The verified identity behind a request.
 *
 * @param actorId when the token is an impersonation token, the real (admin /
 *                tenant-owner) user who is acting as {@code userId}; null otherwise.
 *                Authorization decisions use {@code userId}/{@code role} as usual;
 *                {@code actorId} is for logging and audit.
 */
public record AuthContext(Long userId, String role, String username, Long actorId) {

    public static final AuthContext ANONYMOUS = new AuthContext(null, null, null, null);

    public AuthContext(Long userId, String role, String username) {
        this(userId, role, username, null);
    }

    public boolean isAuthenticated() {
        return userId != null && role != null;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isTenant() {
        return "TENANT".equalsIgnoreCase(role);
    }

    public boolean isUser() {
        return "USER".equalsIgnoreCase(role);
    }

    /** True when someone else holds this session on the user's behalf. */
    public boolean isImpersonated() {
        return actorId != null;
    }
}
