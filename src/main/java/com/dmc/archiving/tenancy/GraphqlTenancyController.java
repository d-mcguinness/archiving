package com.dmc.archiving.tenancy;

import com.dmc.archiving.auth.api.AuthGuard;
import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.input.UpdateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantStatus;
import com.dmc.archiving.tenancy.service.TenancyService;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class GraphqlTenancyController {

    private final TenancyService tenancyService;

    public GraphqlTenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    @QueryMapping
    public List<Tenant> getAllTenants() {
        return tenancyService.getAllTenants();
    }

    @QueryMapping
    public Tenant getTenant(@Argument Long id) {
        return tenancyService.getTenantById(id);
    }

    @QueryMapping
    public List<Tenant> getTenantsByStatus(@Argument TenantStatus status) {
        return tenancyService.getTenantsByStatus(status);
    }

    @QueryMapping
    public List<Tenant> getTenantsByOwner(@Argument Long ownerId) {
        return tenancyService.getTenantsByOwner(ownerId);
    }

    @MutationMapping
    public Tenant createTenant(@Argument CreateTenantInput input, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return tenancyService.createTenant(input);
    }

    @MutationMapping
    public Tenant updateTenant(@Argument UpdateTenantInput input, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return tenancyService.updateTenant(input);
    }

    @MutationMapping
    public Boolean addUserToTenant(@Argument Long userId, @Argument Long tenantId, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        // Let domain failures propagate (e.g. unknown user/tenant -> BAD_REQUEST,
        // seat limit reached -> BAD_REQUEST): GlobalExceptionHandler classifies
        // them into a message-bearing GraphQL error. Swallowing to `false` here
        // hid the cause and defeated that handler.
        tenancyService.addUserToTenant(userId, tenantId);
        return true;
    }

    @MutationMapping
    public Boolean removeUserFromTenant(@Argument Long tenantId, @Argument Long userId, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        tenancyService.removeUserFromTenant(tenantId, userId);
        return true;
    }

    @MutationMapping
    public boolean deleteTenant(@Argument Long id, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return tenancyService.deleteTenant(id);
    }

    @SchemaMapping(typeName = "Tenant", field = "createdAt")
    public String createdAt(Tenant tenant) {
        return formatDateTime(tenant.getCreatedAt());
    }

    @SchemaMapping(typeName = "Tenant", field = "updatedAt")
    public String updatedAt(Tenant tenant) {
        return formatDateTime(tenant.getUpdatedAt());
    }

    @SchemaMapping(typeName = "TenantSettings", field = "maxStorageBytes")
    public String maxStorageBytes(com.dmc.archiving.tenancy.model.TenantSettings settings) {
        Long value = settings.getMaxStorageBytes();
        return value != null ? value.toString() : null;
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
