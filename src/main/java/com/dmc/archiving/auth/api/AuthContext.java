package com.dmc.archiving.auth.api;

public record AuthContext(Long userId, String role, String username) {

    public static final AuthContext ANONYMOUS = new AuthContext(null, null, null);

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
}
