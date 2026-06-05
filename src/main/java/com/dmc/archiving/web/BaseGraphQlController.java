package com.dmc.archiving.web;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.AuthGuard;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseGraphQlController {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final TenancyApi tenancyApi;

    protected BaseGraphQlController(TenancyApi tenancyApi) {
        this.tenancyApi = tenancyApi;
    }

    protected Tenant resolveTenant(Long tenantId, Long entityId, String entityType) {
        if (tenantId == null) {
            return null;
        }
        try {
            return tenancyApi.getTenantById(tenantId);
        } catch (Exception e) {
            log.warn("Could not fetch tenant {} for {} {}: {}",
                tenantId, entityType, entityId, e.getMessage());
            return null;
        }
    }

    protected String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    protected AuthContext getAuthContext(DataFetchingEnvironment env) {
        AuthContext ctx = env.getGraphQlContext().get("authContext");
        return ctx != null ? ctx : AuthContext.ANONYMOUS;
    }

    protected void requireAuthenticated(DataFetchingEnvironment env) {
        AuthGuard.requireAuthenticated(env);
    }

    protected void requireRole(DataFetchingEnvironment env, String... roles) {
        AuthGuard.requireRole(env, roles);
    }

    protected void requireTenantAccess(DataFetchingEnvironment env, Long tenantId) {
        AuthContext ctx = getAuthContext(env);
        if (!ctx.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        if (ctx.isAdmin()) {
            return;
        }
        if (ctx.isTenant() || ctx.isUser()) {
            if (!tenancyApi.isUserInTenant(ctx.userId(), tenantId)) {
                throw new AccessDeniedException("Access denied: user does not belong to tenant " + tenantId);
            }
        }
    }

    protected Long getCurrentUserId(DataFetchingEnvironment env) {
        return getAuthContext(env).userId();
    }
}