package com.dmc.archiving.auth.api;

import graphql.schema.DataFetchingEnvironment;

import java.util.Arrays;

/**
 * Stateless authorization checks for GraphQL resolvers.
 *
 * <p>Role checks depend only on the {@link AuthContext} stashed in the GraphQL
 * context, so this lives in the auth module's public API and can be called from
 * any controller without creating a module cycle (unlike tenant-membership
 * checks, which need the tenancy module and stay on the web base controller).
 */
public final class AuthGuard {

    private AuthGuard() {}

    public static AuthContext context(DataFetchingEnvironment env) {
        AuthContext ctx = env.getGraphQlContext().get("authContext");
        return ctx != null ? ctx : AuthContext.ANONYMOUS;
    }

    public static void requireAuthenticated(DataFetchingEnvironment env) {
        if (!context(env).isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    public static void requireRole(DataFetchingEnvironment env, String... roles) {
        AuthContext ctx = context(env);
        if (!ctx.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        String userRole = ctx.role();
        boolean match = Arrays.stream(roles).anyMatch(r -> r.equalsIgnoreCase(userRole));
        if (!match) {
            throw new AccessDeniedException("Access denied: required role " + Arrays.toString(roles));
        }
    }
}
