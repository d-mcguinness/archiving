package com.dmc.archiving.common;

import com.dmc.archiving.auth.AccessDeniedException;
import com.dmc.archiving.auth.AuthContext;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.service.TenancyService;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public abstract class BaseGraphQlController {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final TenancyService tenancyService;

    protected BaseGraphQlController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    protected Tenant resolveTenant(Long tenantId, Long entityId, String entityType) {
        if (tenantId == null) {
            return null;
        }
        try {
            return tenancyService.getTenantById(tenantId);
        } catch (Exception e) {
            log.warn("Could not fetch tenant {} for {} {}: {}",
                tenantId, entityType, entityId, e.getMessage());
            return null;
        }
    }

    protected String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    // Auth helpers

    protected AuthContext getAuthContext(DataFetchingEnvironment env) {
        AuthContext ctx = env.getGraphQlContext().get("authContext");
        return ctx != null ? ctx : AuthContext.ANONYMOUS;
    }

    protected void requireAuthenticated(DataFetchingEnvironment env) {
        if (!getAuthContext(env).isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
    }

    protected void requireRole(DataFetchingEnvironment env, String... roles) {
        AuthContext ctx = getAuthContext(env);
        if (!ctx.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        String userRole = ctx.role();
        boolean match = Arrays.stream(roles).anyMatch(r -> r.equalsIgnoreCase(userRole));
        if (!match) {
            throw new AccessDeniedException("Access denied: required role " + Arrays.toString(roles));
        }
    }

    protected void requireTenantAccess(DataFetchingEnvironment env, Long tenantId) {
        AuthContext ctx = getAuthContext(env);
        if (!ctx.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (ctx.isAdmin()) {
            return; // Admins can access any tenant
        }
        if (ctx.isTenant() || ctx.isUser()) {
            if (!tenancyService.isUserInTenant(ctx.userId(), tenantId)) {
                throw new AccessDeniedException("Access denied: user does not belong to tenant " + tenantId);
            }
        }
    }

    protected Long getCurrentUserId(DataFetchingEnvironment env) {
        return getAuthContext(env).userId();
    }
}
