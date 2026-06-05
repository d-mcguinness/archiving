package com.dmc.archiving.archive;

import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.input.UpdateArchiveInput;
import com.dmc.archiving.archive.input.AssignUserInput;
import com.dmc.archiving.archive.input.UnassignUserInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.strategy.ArchiveStrategy;
import com.dmc.archiving.archive.strategy.ArchiveStrategyFactory;
import com.dmc.archiving.archive.strategy.ValidationResult;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.tenancy.model.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ArchiveController extends BaseGraphQlController {

    private final ArchiveService archiveService;
    private final ArchiveStrategyFactory strategyFactory;
    private final BillingTenantResolver billingTenantResolver;

    public ArchiveController(ArchiveService archiveService, ArchiveStrategyFactory strategyFactory,
                             TenancyApi tenancyApi, BillingTenantResolver billingTenantResolver) {
        super(tenancyApi);
        this.archiveService = archiveService;
        this.strategyFactory = strategyFactory;
        this.billingTenantResolver = billingTenantResolver;
    }

    // ========== Legacy Query Methods (Non-paginated - use paginated versions for production) ==========

    @QueryMapping
    public List<Archive> getAllArchives() {
        return archiveService.getAllArchives();
    }

    @QueryMapping
    public Archive getArchive(@Argument Long id) {
        return archiveService.getArchiveById(id);
    }

    @QueryMapping
    public List<Archive> getArchivesByUser(@Argument Long userId) {
        return archiveService.getArchivesByUserId(userId);
    }

    @QueryMapping
    public List<Archive> getArchivesByOwner(@Argument Long ownerId) {
        return archiveService.getArchivesByOwner(ownerId);
    }

    @QueryMapping
    public List<Archive> getArchivesByTenant(@Argument Long tenantId) {
        return archiveService.getArchivesByTenant(tenantId);
    }

    @QueryMapping
    public List<Archive> getAllSips() {
        return archiveService.getAllSips();
    }

    @QueryMapping
    public List<Archive> getSipsByTenant(@Argument Long tenantId) {
        return archiveService.getSipsByTenant(tenantId);
    }

    // New query methods for user assignments
    @QueryMapping
    public List<Archive> getArchivesByUserAssignment(@Argument Long userId) {
        return archiveService.getArchivesByUserAssignment(userId);
    }

    // ========== Paginated Query Methods (Recommended for scalability) ==========

    @QueryMapping
    public com.dmc.archiving.archive.dto.ArchivePage getAllArchivesPaginated(
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        // Default values
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        return com.dmc.archiving.archive.dto.ArchivePage.from(archiveService.getAllArchivesPaginated(pageable));
    }

    @QueryMapping
    public com.dmc.archiving.archive.dto.ArchivePage getArchivesByUserPaginated(
            @Argument Long userId,
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        return com.dmc.archiving.archive.dto.ArchivePage.from(archiveService.getArchivesByUserIdPaginated(userId, pageable));
    }

    @QueryMapping
    public com.dmc.archiving.archive.dto.ArchivePage getArchivesByUserAssignmentPaginated(
            @Argument Long userId,
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        return com.dmc.archiving.archive.dto.ArchivePage.from(archiveService.getArchivesByUserAssignmentPaginated(userId, pageable));
    }

    @QueryMapping
    public com.dmc.archiving.archive.dto.ArchivePage getArchivesByStatusPaginated(
            @Argument ArchiveStatus status,
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        return com.dmc.archiving.archive.dto.ArchivePage.from(archiveService.getArchivesByStatusPaginated(status, pageable));
    }

    @QueryMapping
    public com.dmc.archiving.archive.dto.ArchivePage searchArchivesByTitlePaginated(
            @Argument String title,
            @Argument Integer page,
            @Argument Integer size,
            @Argument String sortBy,
            @Argument String sortDirection) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        String sortField = sortBy != null ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));
        return com.dmc.archiving.archive.dto.ArchivePage.from(archiveService.searchArchivesByTitlePaginated(title, pageable));
    }

    // Existing mutation methods
    @MutationMapping
    public Archive createArchive(@Argument CreateArchiveInput input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        input.setTenantId(billingTenantResolver.resolve(getAuthContext(env), input.getTenantId()));
        return archiveService.createArchive(input);
    }

    @MutationMapping
    public Archive updateArchive(@Argument Long id, @Argument UpdateArchiveInput input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.updateArchive(id, input);
    }

    @MutationMapping
    public Archive updateArchiveStatus(@Argument Long archiveId, @Argument ArchiveStatus status, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.updateArchiveStatus(archiveId, status);
    }

    @MutationMapping
    public Archive setArchiveRootElement(@Argument Long archiveId, @Argument Long rootElementId, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.setArchiveRootElement(archiveId, rootElementId);
    }

    @MutationMapping
    public Boolean deleteArchive(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.deleteArchive(id);
    }

    // New mutation methods for user assignment
    @MutationMapping
    public Archive assignUserToArchive(@Argument AssignUserInput input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.assignUserToArchive(input);
    }

    @MutationMapping
    public Archive unassignUserFromArchive(@Argument UnassignUserInput input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return archiveService.unassignUserFromArchive(input);
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Archive", field = "createdAt")
    public String createdAt(Archive archive) {
        return formatDateTime(archive.getCreatedAt());
    }

    @SchemaMapping(typeName = "Archive", field = "updatedAt")
    public String updatedAt(Archive archive) {
        return formatDateTime(archive.getUpdatedAt());
    }

    // Field resolver for tenant
    @SchemaMapping(typeName = "Archive", field = "tenant")
    @SuppressWarnings("ModuleDependency") // Tenant is in same Maven module, IntelliJ false positive
    public Tenant tenant(Archive archive) {
        return resolveTenant(archive.getTenantId(), archive.getId(), "archive");
    }

    // REST Endpoint for Archive Extraction/Download
    @PostMapping("/api/archives/{archiveId}/extract")
    @ResponseBody
    public ResponseEntity<?> extractArchive(
            @PathVariable Long archiveId,
            @RequestBody Map<String, String> request) {

        try {
            String password = request.get("password");

            // Validate password is provided
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "error", "Password is required"
                    ));
            }

            // Get the archive with full element tree
            Archive archive = archiveService.getArchiveForExport(archiveId);
            if (archive == null) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "success", false,
                        "error", "Archive not found"
                    ));
            }

            // TODO: Implement actual password validation
            // For now, accept any password except "wrong"
            if ("wrong".equals(password)) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "success", false,
                        "error", "Invalid password"
                    ));
            }

            // Use strategy pattern to export based on standard
            ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());

            log.info("Extracting archive {} using {} strategy", archiveId, strategy.getStandardName());

            // Export using standard-specific strategy
            Map<String, Object> exportData = strategy.export(archive);

            // Convert to JSON with Java 8 date/time support
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String archiveJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment",
                "archive_" + archiveId + "_" + archive.getStandard().name() + "_export.json");

            return ResponseEntity
                .ok()
                .headers(headers)
                .body(archiveJson);

        } catch (Exception e) {
            // Log the full exception for debugging
            log.error("Failed to extract archive {}: {}", archiveId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to extract archive: " + e.getMessage()
                ));
        }
    }

    // REST Endpoint for Archive Validation
    @PostMapping("/api/archives/{archiveId}/validate")
    @ResponseBody
    public ResponseEntity<?> validateArchive(@PathVariable Long archiveId) {
        try {
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Archive not found"));
            }

            // Use strategy pattern for validation
            ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());
            ValidationResult result = strategy.validate(archive);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("standard", strategy.getStandardName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to validate archive {}: {}", archiveId, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Validation failed: " + e.getMessage()));
        }
    }

    // REST Endpoint for Getting Metadata Requirements
    @GetMapping("/api/standards/{standardName}/requirements")
    @ResponseBody
    public ResponseEntity<?> getMetadataRequirements(@PathVariable String standardName) {
        try {
            ArchiveStrategy strategy = strategyFactory.getStrategy(standardName);
            Map<String, String> requirements = strategy.getMetadataRequirements();

            Map<String, Object> response = new HashMap<>();
            response.put("standard", strategy.getStandardName());
            response.put("requirements", requirements);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get requirements for {}: {}", standardName, e.getMessage(), e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", "Failed to get requirements"));
        }
    }
}
