package com.dmc.archiving.aip;

import com.dmc.archiving.aip.input.CreateAipInput;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.web.BaseGraphQlController;
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
public class AipController extends BaseGraphQlController {

    private final AipService aipService;

    public AipController(AipService aipService, TenancyApi tenancyApi) {
        super(tenancyApi);
        this.aipService = aipService;
    }

    // Queries
    @QueryMapping
    public List<Aip> getAllAips() {
        return aipService.getAllAips();
    }

    @QueryMapping
    public List<Aip> getAipsByTenant(@Argument Long tenantId) {
        return aipService.getAipsByTenant(tenantId);
    }

    @QueryMapping
    public Aip getAip(@Argument Long id) {
        return aipService.getAip(id);
    }

    // Mutations
    @MutationMapping
    public Aip createAip(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        CreateAipInput aipInput = new CreateAipInput();

        aipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        aipInput.setTitle(input.get("title").toString());
        aipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        if (input.get("tenantId") != null) {
            aipInput.setTenantId(Long.parseLong(input.get("tenantId").toString()));
        }
        if (input.get("ownerId") != null) {
            aipInput.setOwnerId(Long.parseLong(input.get("ownerId").toString()));
        }
        if (input.get("description") != null) {
            aipInput.setDescription(input.get("description").toString());
        }
        if (input.get("content") != null) {
            aipInput.setContent(input.get("content").toString());
        }
        if (input.get("sourceSipId") != null) {
            aipInput.setSourceSipId(Long.parseLong(input.get("sourceSipId").toString()));
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

        return aipService.createAip(aipInput);
    }

    @MutationMapping
    public String generateAip(@Argument Long aipId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return aipService.generateAip(aipId);
    }

    @MutationMapping
    public Aip updateAipStatus(@Argument Long aipId, @Argument AipStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return aipService.updateAipStatus(aipId, status);
    }

    @MutationMapping
    public Boolean deleteAip(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return aipService.deleteAip(id);
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Aip", field = "createdAt")
    public String createdAt(Aip aip) {
        return formatDateTime(aip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Aip", field = "updatedAt")
    public String updatedAt(Aip aip) {
        return formatDateTime(aip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Aip", field = "tenant")
    public Tenant tenant(Aip aip) {
        return resolveTenant(aip.getTenantId(), aip.getId(), "aip");
    }
}
