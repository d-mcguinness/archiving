package com.dmc.archiving.sip;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.sip.input.CreateSipInput;
import com.dmc.archiving.sip.input.FileMetadataInput;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.sip.model.SipStatus;
import com.dmc.archiving.sip.generator.SipGeneratorFactory;
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
import java.util.stream.Collectors;

@Controller
public class SipController extends BaseGraphQlController {

    private final SipService sipService;
    private final SipGeneratorFactory sipGeneratorFactory;

    public SipController(SipService sipService, SipGeneratorFactory sipGeneratorFactory, TenancyApi tenancyApi) {
        super(tenancyApi);
        this.sipService = sipService;
        this.sipGeneratorFactory = sipGeneratorFactory;
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

    @QueryMapping
    public List<Map<String, String>> prefillSipFields(@Argument ArchiveStandard standard, @Argument Map<String, Object> fileMetadata) {
        FileMetadataInput meta = new FileMetadataInput();
        meta.setFilename((String) fileMetadata.get("filename"));
        meta.setContentType((String) fileMetadata.get("contentType"));
        meta.setFileSize(Long.parseLong(fileMetadata.get("fileSize").toString()));
        meta.setChecksum((String) fileMetadata.get("checksum"));
        meta.setUploadedAt((String) fileMetadata.get("uploadedAt"));
        meta.setUploaderName((String) fileMetadata.get("uploaderName"));
        meta.setFileCount(Integer.parseInt(fileMetadata.get("fileCount").toString()));

        Map<String, String> fields = sipGeneratorFactory.getGenerator(standard).prefillFields(meta);
        return fields.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
                .collect(Collectors.toList());
    }

    // Mutations
    @MutationMapping
    public Sip createSipV2(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
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
        if (input.get("archiveId") != null) {
            sipInput.setArchiveId(Long.parseLong(input.get("archiveId").toString()));
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
    public String generateSip(@Argument Long sipId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return sipService.generateSip(sipId);
    }

    @MutationMapping
    public Sip updateSipStatusV2(@Argument Long sipId, @Argument SipStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return sipService.updateSipStatus(sipId, status);
    }

    @MutationMapping
    public Boolean deleteSipV2(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
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
