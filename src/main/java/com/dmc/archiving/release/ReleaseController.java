package com.dmc.archiving.release;

import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.release.input.CreateReleaseInput;
import com.dmc.archiving.release.model.Release;
import com.dmc.archiving.release.model.ReleaseStatus;
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
public class ReleaseController extends BaseGraphQlController {

    private final ReleaseService dipService;
    private final BillingTenantResolver billingTenantResolver;

    public ReleaseController(ReleaseService dipService, TenancyApi tenancyApi,
                         BillingTenantResolver billingTenantResolver) {
        super(tenancyApi);
        this.dipService = dipService;
        this.billingTenantResolver = billingTenantResolver;
    }

    // Queries
    @QueryMapping
    public List<Release> getAllReleases() {
        return dipService.getAllReleases();
    }

    @QueryMapping
    public List<Release> getReleasesByTenant(@Argument Long tenantId) {
        return dipService.getReleasesByTenant(tenantId);
    }

    @QueryMapping
    public Release getRelease(@Argument Long id) {
        return dipService.getRelease(id);
    }

    // Mutations
    @MutationMapping
    public Release createRelease(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        CreateReleaseInput dipInput = new CreateReleaseInput();

        dipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        dipInput.setTitle(input.get("title").toString());
        dipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        Long claimedTenantId = input.get("tenantId") != null
                ? Long.parseLong(input.get("tenantId").toString()) : null;
        dipInput.setTenantId(billingTenantResolver.resolve(getAuthContext(env), claimedTenantId));
        // ADMIN/operator-created packages are not billed to the tenant. The
        // premium-package spend cap is enforced inside ReleaseService.createRelease
        // (same transaction as the insert).
        dipInput.setBillable(!getAuthContext(env).isAdmin());
        if (input.get("ownerId") != null) {
            dipInput.setOwnerId(Long.parseLong(input.get("ownerId").toString()));
        }
        if (input.get("description") != null) {
            dipInput.setDescription(input.get("description").toString());
        }
        if (input.get("content") != null) {
            dipInput.setContent(input.get("content").toString());
        }
        if (input.get("sourcePreservationId") != null) {
            dipInput.setSourcePreservationId(Long.parseLong(input.get("sourcePreservationId").toString()));
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

        return dipService.createRelease(dipInput);
    }

    @MutationMapping
    public String generateRelease(@Argument Long releaseId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, releaseId);
        return dipService.generateRelease(releaseId);
    }

    @MutationMapping
    public Release updateReleaseStatus(@Argument Long releaseId, @Argument ReleaseStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, releaseId);
        return dipService.updateReleaseStatus(releaseId, status);
    }

    @MutationMapping
    public Boolean deleteRelease(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, id);
        return dipService.deleteRelease(id);
    }

    /** A TENANT/USER may only touch a DIP in a tenant they belong to (ADMIN bypasses). */
    private void requireOwnership(DataFetchingEnvironment env, Long releaseId) {
        Release dip = dipService.getRelease(releaseId);
        if (dip == null) {
            throw new ResourceNotFoundException("Release", releaseId);
        }
        requireTenantAccess(env, dip.getTenantId());
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Release", field = "createdAt")
    public String createdAt(Release dip) {
        return formatDateTime(dip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Release", field = "updatedAt")
    public String updatedAt(Release dip) {
        return formatDateTime(dip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Release", field = "tenant")
    public Tenant tenant(Release dip) {
        return resolveTenant(dip.getTenantId(), dip.getId(), "dip");
    }
}
