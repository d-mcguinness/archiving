package com.dmc.archiving.sip;

import com.dmc.archiving.common.BaseGraphQlController;
import com.dmc.archiving.sip.input.CreateSipInput;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.sip.model.SipStatus;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.service.TenancyService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class SipController extends BaseGraphQlController {

    private final SipService sipService;

    public SipController(SipService sipService, TenancyService tenancyService) {
        super(tenancyService);
        this.sipService = sipService;
    }

    // Queries
    @QueryMapping
    public List<Sip> getAllSipsV2() {
        return sipService.getAllSips();
    }

    @QueryMapping
    public List<Sip> getSipsByTenantV2(@Argument Long tenantId) {
        return sipService.getSipsByTenant(tenantId);
    }

    @QueryMapping
    public Sip getSip(@Argument Long id) {
        return sipService.getSip(id);
    }

    // Mutations
    @MutationMapping
    public Sip createSipV2(@Argument Map<String, Object> input) {
        CreateSipInput sipInput = new CreateSipInput();

        sipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        sipInput.setTitle(input.get("title").toString());
        sipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        if (input.get("tenantId") != null) {
            sipInput.setTenantId(Long.parseLong(input.get("tenantId").toString()));
        }
        if (input.get("ownerId") != null) {
            sipInput.setOwnerId(Long.parseLong(input.get("ownerId").toString()));
        }
        if (input.get("description") != null) {
            sipInput.setDescription(input.get("description").toString());
        }
        if (input.get("content") != null) {
            sipInput.setContent(input.get("content").toString());
        }

        // Root element fields
        sipInput.setElementIdentifier(input.get("elementIdentifier").toString());
        sipInput.setEntityName(input.get("entityName").toString());
        sipInput.setEntityType(input.get("entityType").toString());
        sipInput.setElementTitle(input.get("elementTitle").toString());
        sipInput.setCreatedBy(input.get("createdBy").toString());

        if (input.get("norwegianName") != null) {
            sipInput.setNorwegianName(input.get("norwegianName").toString());
        }
        if (input.get("englishName") != null) {
            sipInput.setEnglishName(input.get("englishName").toString());
        }
        if (input.get("elementDescription") != null) {
            sipInput.setElementDescription(input.get("elementDescription").toString());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = input.get("fields") != null ?
            (List<Map<String, Object>>) input.get("fields") : null;
        sipInput.setFields(fields);

        return sipService.createSip(sipInput);
    }

    @MutationMapping
    public String generateSip(@Argument Long sipId) {
        return sipService.generateSip(sipId);
    }

    @MutationMapping
    public Sip updateSipStatusV2(@Argument Long sipId, @Argument SipStatus status) {
        return sipService.updateSipStatus(sipId, status);
    }

    @MutationMapping
    public Boolean deleteSipV2(@Argument Long id) {
        return sipService.deleteSip(id);
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Sip", field = "createdAt")
    public String createdAt(Sip sip) {
        return formatDateTime(sip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Sip", field = "updatedAt")
    public String updatedAt(Sip sip) {
        return formatDateTime(sip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Sip", field = "tenant")
    public Tenant tenant(Sip sip) {
        return resolveTenant(sip.getTenantId(), sip.getId(), "sip");
    }
}
