package com.dmc.archiving.tenancy;

import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.input.UpdateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantStatus;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class GraphqlTenancyController implements TenancyApi {

    private final TenancyService tenancyService;

    public GraphqlTenancyController(TenancyService tenancyService) {
        this.tenancyService = tenancyService;
    }

    // Query operations
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

    @Override
    public boolean isTenantActive(Long tenantId) {
        return tenancyService.isTenantActive(tenantId);
    }

    @Override
    public Tenant getTenantById(Long tenantId) {
        return tenancyService.getTenantById(tenantId);
    }

    @Override
    public boolean isUserInTenant(Long userId, Long tenantId) {
        return tenancyService.isUserInTenant(userId, tenantId);
    }

    // Mutation operations
    @MutationMapping
    public Tenant createTenant(@Argument CreateTenantInput input) {
        return tenancyService.createTenant(input);
    }

    @MutationMapping
    public Tenant updateTenant(@Argument UpdateTenantInput input) {
        return tenancyService.updateTenant(input);
    }

    @MutationMapping
    public Boolean addUserToTenant(@Argument Long userId, @Argument Long tenantId) {
        try {
            tenancyService.addUserToTenant(userId, tenantId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @MutationMapping
    public Boolean removeUserFromTenant(@Argument Long userId) {
        try {
            tenancyService.removeUserFromTenant(userId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @MutationMapping
    public boolean deleteTenant(@Argument Long id) {
        return tenancyService.deleteTenant(id);
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Tenant", field = "createdAt")
    public String createdAt(Tenant tenant) {
        return tenant.getCreatedAt() != null ?
            tenant.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @SchemaMapping(typeName = "Tenant", field = "updatedAt")
    public String updatedAt(Tenant tenant) {
        return tenant.getUpdatedAt() != null ?
            tenant.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    // Map TenantSettings.maxStorageBytes (Long) to String for GraphQL schema
    @SchemaMapping(typeName = "TenantSettings", field = "maxStorageBytes")
    public String maxStorageBytes(com.dmc.archiving.tenancy.model.TenantSettings settings) {
        Long value = settings.getMaxStorageBytes();
        return value != null ? value.toString() : null;
    }
}
