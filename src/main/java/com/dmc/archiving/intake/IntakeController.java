package com.dmc.archiving.intake;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.intake.input.CreateIntakeInput;
import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.intake.model.IntakeStatus;
import com.dmc.archiving.intake.generator.IntakeGeneratorFactory;
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
import java.util.stream.Collectors;

@Controller
public class IntakeController extends BaseGraphQlController {

    private final IntakeService sipService;
    private final IntakeGeneratorFactory sipGeneratorFactory;
    private final BillingTenantResolver billingTenantResolver;

    public IntakeController(IntakeService sipService, IntakeGeneratorFactory sipGeneratorFactory,
                         TenancyApi tenancyApi, BillingTenantResolver billingTenantResolver) {
        super(tenancyApi);
        this.sipService = sipService;
        this.sipGeneratorFactory = sipGeneratorFactory;
        this.billingTenantResolver = billingTenantResolver;
    }

    // Queries
    @QueryMapping
    public List<Intake> getAllIntakesV2() {
        return sipService.getAllIntakes();
    }

    @QueryMapping
    public List<Intake> getIntakesByTenantV2(@Argument Long tenantId) {
        return sipService.getIntakesByTenant(tenantId);
    }

    @QueryMapping
    public Intake getIntake(@Argument Long id) {
        return sipService.getIntake(id);
    }

    @QueryMapping
    public List<Map<String, String>> prefillIntakeFields(@Argument ArchiveStandard standard, @Argument Map<String, Object> fileMetadata) {
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
    public Intake createIntakeV2(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        CreateIntakeInput sipInput = new CreateIntakeInput();

        sipInput.setUserId(Long.parseLong(input.get("userId").toString()));
        sipInput.setTitle(input.get("title").toString());
        sipInput.setStandard(com.dmc.archiving.archive.model.ArchiveStandard.valueOf(input.get("standard").toString()));

        Long claimedTenantId = input.get("tenantId") != null
                ? Long.parseLong(input.get("tenantId").toString()) : null;
        sipInput.setTenantId(billingTenantResolver.resolve(getAuthContext(env), claimedTenantId));
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

        return sipService.createIntake(sipInput);
    }

    @MutationMapping
    public String generateIntake(@Argument Long intakeId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, intakeId);
        return sipService.generateIntake(intakeId);
    }

    @MutationMapping
    public Intake updateIntakeStatusV2(@Argument Long intakeId, @Argument IntakeStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, intakeId);
        return sipService.updateIntakeStatus(intakeId, status);
    }

    @MutationMapping
    public Boolean deleteIntakeV2(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        requireOwnership(env, id);
        return sipService.deleteIntake(id);
    }

    /** A TENANT/USER may only touch a SIP in a tenant they belong to (ADMIN bypasses). */
    private void requireOwnership(DataFetchingEnvironment env, Long intakeId) {
        Intake sip = sipService.getIntake(intakeId);
        if (sip == null) {
            throw new ResourceNotFoundException("Intake", intakeId);
        }
        requireTenantAccess(env, sip.getTenantId());
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Intake", field = "createdAt")
    public String createdAt(Intake sip) {
        return formatDateTime(sip.getCreatedAt());
    }

    @SchemaMapping(typeName = "Intake", field = "updatedAt")
    public String updatedAt(Intake sip) {
        return formatDateTime(sip.getUpdatedAt());
    }

    @SchemaMapping(typeName = "Intake", field = "tenant")
    public Tenant tenant(Intake sip) {
        return resolveTenant(sip.getTenantId(), sip.getId(), "sip");
    }
}
