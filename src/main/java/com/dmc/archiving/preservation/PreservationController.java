package com.dmc.archiving.preservation;

import com.dmc.archiving.preservation.input.CreatePreservationInput;
import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.preservation.model.PreservationStatus;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import org.springframework.graphql.data.method.annotation.Argument;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class PreservationController extends BaseGraphQlController {

    private final PreservationService aipService;
    private final BillingTenantResolver billingTenantResolver;

    public PreservationController(PreservationService aipService, TenancyApi tenancyApi,
                         BillingTenantResolver billingTenantResolver) {
        super(tenancyApi);
        this.aipService = aipService;
        this.billingTenantResolver = billingTenantResolver;
    }

    // Queries
    @QueryMapping
    public List<Preservation> getAllPreservations() {
        return aipService.getAllPreservations();
    }

    @QueryMapping
    public List<Preservation> getPreservationsByTenant(@Argument Long tenantId) {
        return aipService.getPreservationsByTenant(tenantId);
    }

    @QueryMapping
    public Preservation getPreservation(@Argument Long id) {
        return aipService.getPreservation(id);
    }

    // Mutations
    @MutationMapping
    public Preservation createPreservation(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        CreatePreservationInput aipInput = new CreatePreservationInput();

        aipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        aipInput.setTitle(input.get("title").toString());
        aipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        Long claimedTenantId = input.get("tenantId") != null
                ? Long.parseLong(input.get("tenantId").toString()) : null;
        aipInput.setTenantId(billingTenantResolver.resolve(getAuthContext(env), claimedTenantId));
        // ADMIN/operator-created packages are not billed to the tenant. The
        // premium-package spend cap is enforced inside PreservationService.createPreservation
        // (same transaction as the insert).
        aipInput.setBillable(!getAuthContext(env).isAdmin());
        if (input.get("ownerId") != null) {
            aipInput.setOwnerId(Long.parseLong(input.get("ownerId").toString()));
        }
        if (input.get("description") != null) {
            aipInput.setDescription(input.get("description").toString());
        }
        if (input.get("content") != null) {
            aipInput.setContent(input.get("content").toString());
        }
        if (input.get("sourceIntakeId") != null) {
            aipInput.setSourceIntakeId(Long.parseLong(input.get("sourceIntakeId").toString()));
        }

        // Root element fields
        aipInput.setElementIdentifier(input.get("elementIdentifier").toString());
        aipInput.setEntityName(input.get("entityName").toString());
        aipInput.setEntityType(input.get("entityType").toString());
        aipInput.setElementTitle(input.get("elementTitle").toString());
        aipInput.setCreatedBy(input.get("createdBy").toString());

        if (input.get("norwegianName") != null) {
            aipInput.setNorwegianName(input.get("norwegianName").toString());
        }
        if (input.get("englishName") != null) {
            aipInput.setEnglishName(input.get("englishName").toString());
        }
        if (input.get("elementDescription") != null) {
            aipInput.setElementDescription(input.get("elementDescription").toString());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = input.get("fields") != null ?
            (List<Map<String, Object>>) input.get("fields") : null;
        aipInput.setFields(fields);

        return aipService.createPreservation(aipInput);
    }

    @MutationMapping
    public String generatePreservation(@Argument Long preservationId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, preservationId);
        return aipService.generatePreservation(preservationId);
    }

    @MutationMapping
    public Preservation updatePreservationStatus(@Argument Long preservationId, @Argument PreservationStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, preservationId);
        return aipService.updatePreservationStatus(preservationId, status);
    }

    @MutationMapping
    public Boolean deletePreservation(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, id);
        return aipService.deletePreservation(id);
    }

    /** A TENANT/USER may only touch an AIP in a tenant they belong to (ADMIN bypasses). */
    private void requireOwnership(DataFetchingEnvironment env, Long preservationId) {
        Preservation aip = aipService.getPreservation(preservationId);
        if (aip == null) {
            throw new ResourceNotFoundException("Preservation", preservationId);
        }
        requireTenantAccess(env, aip.getTenantId());
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Preservation", field = "createdAt")
    public String createdAt(Preservation aip) {
        return formatDateTime(aip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Preservation", field = "updatedAt")
    public String updatedAt(Preservation aip) {
        return formatDateTime(aip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Preservation", field = "tenant")
    public Tenant tenant(Preservation aip) {
        return resolveTenant(aip.getTenantId(), aip.getId(), "aip");
    }
}
