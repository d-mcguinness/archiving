package com.dmc.archiving.dip;

import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.dip.input.CreateDipInput;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.dip.model.DipStatus;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.PremiumOverageGuard;
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
public class DipController extends BaseGraphQlController {

    private final DipService dipService;
    private final BillingTenantResolver billingTenantResolver;
    private final PremiumOverageGuard premiumOverageGuard;

    public DipController(DipService dipService, TenancyApi tenancyApi,
                         BillingTenantResolver billingTenantResolver,
                         PremiumOverageGuard premiumOverageGuard) {
        super(tenancyApi);
        this.dipService = dipService;
        this.billingTenantResolver = billingTenantResolver;
        this.premiumOverageGuard = premiumOverageGuard;
    }

    // Queries
    @QueryMapping
    public List<Dip> getAllDips() {
        return dipService.getAllDips();
    }

    @QueryMapping
    public List<Dip> getDipsByTenant(@Argument Long tenantId) {
        return dipService.getDipsByTenant(tenantId);
    }

    @QueryMapping
    public Dip getDip(@Argument Long id) {
        return dipService.getDip(id);
    }

    // Mutations
    @MutationMapping
    public Dip createDip(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        CreateDipInput dipInput = new CreateDipInput();

        dipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        dipInput.setTitle(input.get("title").toString());
        dipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        Long claimedTenantId = input.get("tenantId") != null
                ? Long.parseLong(input.get("tenantId").toString()) : null;
        dipInput.setTenantId(billingTenantResolver.resolve(getAuthContext(env), claimedTenantId));
        // ADMIN/operator-created packages are not billed to the tenant.
        dipInput.setBillable(!getAuthContext(env).isAdmin());
        // Enforce the premium-package spend cap for billable premium generations.
        if (dipInput.isBillable() && premiumOverageGuard.isPremiumStandard(input.get("standard").toString())) {
            premiumOverageGuard.checkCanCreatePremiumPackage(dipInput.getTenantId());
        }
        if (input.get("ownerId") != null) {
            dipInput.setOwnerId(Long.parseLong(input.get("ownerId").toString()));
        }
        if (input.get("description") != null) {
            dipInput.setDescription(input.get("description").toString());
        }
        if (input.get("content") != null) {
            dipInput.setContent(input.get("content").toString());
        }
        if (input.get("sourceAipId") != null) {
            dipInput.setSourceAipId(Long.parseLong(input.get("sourceAipId").toString()));
        }

        // Root element fields
        dipInput.setElementIdentifier(input.get("elementIdentifier").toString());
        dipInput.setEntityName(input.get("entityName").toString());
        dipInput.setEntityType(input.get("entityType").toString());
        dipInput.setElementTitle(input.get("elementTitle").toString());
        dipInput.setCreatedBy(input.get("createdBy").toString());

        if (input.get("norwegianName") != null) {
            dipInput.setNorwegianName(input.get("norwegianName").toString());
        }
        if (input.get("englishName") != null) {
            dipInput.setEnglishName(input.get("englishName").toString());
        }
        if (input.get("elementDescription") != null) {
            dipInput.setElementDescription(input.get("elementDescription").toString());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = input.get("fields") != null ?
            (List<Map<String, Object>>) input.get("fields") : null;
        dipInput.setFields(fields);

        return dipService.createDip(dipInput);
    }

    @MutationMapping
    public String generateDip(@Argument Long dipId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, dipId);
        return dipService.generateDip(dipId);
    }

    @MutationMapping
    public Dip updateDipStatus(@Argument Long dipId, @Argument DipStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, dipId);
        return dipService.updateDipStatus(dipId, status);
    }

    @MutationMapping
    public Boolean deleteDip(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, id);
        return dipService.deleteDip(id);
    }

    /** A TENANT/USER may only touch a DIP in a tenant they belong to (ADMIN bypasses). */
    private void requireOwnership(DataFetchingEnvironment env, Long dipId) {
        Dip dip = dipService.getDip(dipId);
        if (dip == null) {
            throw new ResourceNotFoundException("Dip", dipId);
        }
        requireTenantAccess(env, dip.getTenantId());
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Dip", field = "createdAt")
    public String createdAt(Dip dip) {
        return formatDateTime(dip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Dip", field = "updatedAt")
    public String updatedAt(Dip dip) {
        return formatDateTime(dip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Dip", field = "tenant")
    public Tenant tenant(Dip dip) {
        return resolveTenant(dip.getTenantId(), dip.getId(), "dip");
    }
}
