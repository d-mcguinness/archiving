package com.dmc.archiving.archive;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.ElementRepository;
import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.input.UpdateArchiveInput;
import com.dmc.archiving.archive.input.AssignUserInput;
import com.dmc.archiving.archive.input.UnassignUserInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.model.UserRole;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.user.api.UserApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final UserApi userApi;
    private final ElementRepository elementRepository;

    public ArchiveService(ArchiveRepository archiveRepository, UserApi userApi, ElementRepository elementRepository) {
        this.archiveRepository = archiveRepository;
        this.userApi = userApi;
        this.elementRepository = elementRepository;
    }

    @Transactional
    public Archive createArchive(CreateArchiveInput input) {
        // Validate that user exists using the public API
        if (!userApi.userExists(input.getUserId())) {
            throw new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist");
        }

        LocalDateTime now = LocalDateTime.now();

        Archive archive = new Archive(
            null, // Let JPA generate the ID
            input.getTenantId(),  // Set tenantId (organization)
            input.getOwnerId(),   // Set ownerId (user who owns it)
            input.getTitle(),
            input.getDescription(),
            input.getContent(),
            now,
            now,
            ArchiveStatus.DRAFT,
            input.getStandard()
        );

        // Automatically assign the creator as OWNER
        archive.assignUser(input.getUserId(), UserRole.OWNER);

        return archiveRepository.save(archive);
    }

    @Transactional
    public Archive assignUserToArchive(AssignUserInput input) {
        // Validate that user exists using the public API
        if (!userApi.userExists(input.getUserId())) {
            throw new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist");
        }

        Archive archive = archiveRepository.findById(input.getArchiveId())
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + input.getArchiveId() + " does not exist"));

        // Assign the user with the specified role
        archive.assignUser(input.getUserId(), input.getRole());
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    @Transactional
    public Archive unassignUserFromArchive(UnassignUserInput input) {
        Archive archive = archiveRepository.findById(input.getArchiveId())
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + input.getArchiveId() + " does not exist"));

        // Don't allow unassigning the owner
        if (archive.getOwnerId().equals(input.getUserId())) {
            throw new IllegalArgumentException("Cannot unassign the owner from the archive");
        }

        archive.unassignUser(input.getUserId());
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    public List<Archive> getArchivesByUserAssignment(Long userId) {
        return archiveRepository.findArchivesByUserIdOwnerOrAssigned(userId);
    }

    public List<Archive> getArchivesByOwner(Long ownerId) {
        return archiveRepository.findByOwnerId(ownerId);
    }

    public List<Archive> getArchivesByUserRole(Long userId, UserRole role) {
        if (role.equals(UserRole.OWNER)) {
            // For owner role, check both actual owner and assigned owner role
            List<Archive> ownedArchives = archiveRepository.findByOwnerId(userId);
            List<Archive> assignedAsOwner = archiveRepository.findArchivesByUserIdAndRole(userId, UserRole.OWNER);
            ownedArchives.addAll(assignedAsOwner);
            return ownedArchives.stream().distinct().toList();
        } else {
            return archiveRepository.findArchivesByUserIdAndRole(userId, role);
        }
    }

    public Archive getArchiveById(Long id) {
        return archiveRepository.findById(id).orElse(null);
    }

    public Archive getArchiveByIdWithRelations(Long id) {
        return archiveRepository.findByIdWithRelations(id);
    }

    // ========== Paginated Query Methods (Recommended for scalability) ==========

    public Page<Archive> getAllArchivesPaginated(Pageable pageable) {
        return archiveRepository.findAll(pageable);
    }

    public Page<Archive> getArchivesByUserIdPaginated(Long userId, Pageable pageable) {
        return archiveRepository.findByOwnerId(userId, pageable);
    }

    public Page<Archive> getArchivesByUserAssignmentPaginated(Long userId, Pageable pageable) {
        return archiveRepository.findArchivesByUserIdOwnerOrAssigned(userId, pageable);
    }

    public Page<Archive> getArchivesByUserRolePaginated(Long userId, UserRole role, Pageable pageable) {
        return archiveRepository.findArchivesByUserIdAndRole(userId, role, pageable);
    }

    public Page<Archive> getArchivesByStatusPaginated(ArchiveStatus status, Pageable pageable) {
        return archiveRepository.findByStatus(status, pageable);
    }

    public Page<Archive> searchArchivesByTitlePaginated(String title, Pageable pageable) {
        return archiveRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Archive> getArchivesByOwnerAndStatusPaginated(Long ownerId, ArchiveStatus status, Pageable pageable) {
        return archiveRepository.findByOwnerIdAndStatus(ownerId, status, pageable);
    }

    // ========== Legacy Non-Paginated Methods (For backward compatibility) ==========
    // NOTE: These should be deprecated in favor of paginated versions for production use

    public List<Archive> getAllArchives() {
        return archiveRepository.findAll();
    }

    public List<Archive> getArchivesByUserId(Long userId) {
        // Return archives where user is owner (for backward compatibility)
        return archiveRepository.findByOwnerId(userId);
    }

    @Transactional
    public Archive updateArchiveStatus(Long archiveId, ArchiveStatus status) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive != null) {
            archive.setStatus(status);
            archive.setUpdatedAt(LocalDateTime.now());
            return archiveRepository.save(archive);
        }
        return null;
    }

    @Transactional
    public Archive updateArchive(Long id, UpdateArchiveInput input) {
        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + id + " does not exist"));

        archive.setTitle(input.getTitle());
        archive.setDescription(input.getDescription());
        archive.setContent(input.getContent());
        archive.setStatus(input.getStatus());
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    @Transactional
    public Archive setArchiveRootElement(Long archiveId, Long rootElementId) {
        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + archiveId + " does not exist"));

        Element rootElement = elementRepository.findById(rootElementId)
                .orElseThrow(() -> new IllegalArgumentException("Element with ID " + rootElementId + " does not exist"));

        // Verify the element belongs to this archive
        if (!rootElement.getArchive().getId().equals(archiveId)) {
            throw new IllegalArgumentException("Element does not belong to this archive");
        }

        // Set the root element
        archive.setRootElement(rootElement);
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    @Transactional
    public Boolean deleteArchive(Long id) {
        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + id + " does not exist"));

        // Delete the archive
        // JPA will automatically cascade delete all associated elements, fields, and user assignments
        // due to @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        archiveRepository.delete(archive);
        return true;
    }

    // Additional convenience methods using JPA repository
    public List<Archive> getArchivesByStatus(ArchiveStatus status) {
        return archiveRepository.findByStatus(status);
    }

    public List<Archive> searchArchivesByTitle(String title) {
        return archiveRepository.findByTitleContainingIgnoreCase(title);
    }

    public List<Archive> getArchivesByOwnerAndStatus(Long ownerId, ArchiveStatus status) {
        return archiveRepository.findByOwnerIdAndStatus(ownerId, status);
    }

    /**
     * Count archives by status
     */
    public int countByStatus(ArchiveStatus status) {
        return archiveRepository.findByStatus(status).size();
    }

    /**
     * Get archive count grouped by standard
     */
    public Map<String, Long> getArchiveCountByStandard() {
        return archiveRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                archive -> archive.getStandard().name(),
                Collectors.counting()
            ));
    }

    /**
     * Export archive as JSON for download
     * This method creates a comprehensive JSON export of the archive including all elements
     */
    public String exportArchiveAsJson(Archive archive) {
        try {
            // Create object mapper with pretty printing
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Build export data structure
            Map<String, Object> exportData = new HashMap<>();

            // Archive metadata
            exportData.put("archiveId", archive.getId());
            exportData.put("title", archive.getTitle());
            exportData.put("description", archive.getDescription());
            exportData.put("content", archive.getContent());
            exportData.put("standard", archive.getStandard());
            exportData.put("status", archive.getStatus());
            exportData.put("ownerId", archive.getOwnerId());
            exportData.put("createdAt", archive.getCreatedAt());
            exportData.put("updatedAt", archive.getUpdatedAt());

            // User assignments
            if (archive.getAssignedUsers() != null && !archive.getAssignedUsers().isEmpty()) {
                List<Map<String, Object>> assignments = archive.getAssignedUsers().stream()
                    .map(assignment -> {
                        Map<String, Object> assignmentData = new HashMap<>();
                        assignmentData.put("userId", assignment.getUserId());
                        assignmentData.put("role", assignment.getRole());
                        assignmentData.put("assignedAt", assignment.getAssignedAt());
                        return assignmentData;
                    })
                    .collect(Collectors.toList());
                exportData.put("assignedUsers", assignments);
            }

            // Elements (if any)
            List<Element> elements = elementRepository.findByArchiveIdOrderByCreatedAtAsc(archive.getId());
            if (!elements.isEmpty()) {
                List<Map<String, Object>> elementsData = elements.stream()
                    .map(element -> {
                        Map<String, Object> elementData = new HashMap<>();
                        elementData.put("id", element.getId());
                        elementData.put("elementIdentifier", element.getElementIdentifier());
                        elementData.put("title", element.getTitle());
                        elementData.put("description", element.getDescription());
                        elementData.put("entityName", element.getEntityName());
                        elementData.put("entityType", element.getEntityType());
                        elementData.put("norwegianName", element.getNorwegianName());
                        elementData.put("englishName", element.getEnglishName());
                        elementData.put("parentId", element.getParent() != null ? element.getParent().getId() : null);
                        elementData.put("status", element.getStatus());
                        elementData.put("createdBy", element.getCreatedBy());
                        elementData.put("createdAt", element.getCreatedAt());
                        elementData.put("updatedAt", element.getUpdatedAt());

                        // Include field values if present - serialize manually to avoid lazy loading issues
                        if (element.getFields() != null && !element.getFields().isEmpty()) {
                            List<Map<String, Object>> fieldsData = element.getFields().stream()
                                .map(field -> {
                                    Map<String, Object> fieldMap = new HashMap<>();
                                    fieldMap.put("id", field.getId());
                                    fieldMap.put("name", field.getName());
                                    fieldMap.put("label", field.getLabel());
                                    fieldMap.put("type", field.getType());
                                    fieldMap.put("value", field.getValue());
                                    return fieldMap;
                                })
                                .collect(Collectors.toList());
                            elementData.put("fields", fieldsData);
                        }

                        return elementData;
                    })
                    .collect(Collectors.toList());
                exportData.put("elements", elementsData);
                exportData.put("elementsCount", elements.size());
            } else {
                exportData.put("elementsCount", 0);
            }

            // Add export metadata
            exportData.put("exportedAt", LocalDateTime.now());
            exportData.put("exportVersion", "1.0");

            // Convert to JSON string
            return mapper.writeValueAsString(exportData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to export archive as JSON: " + e.getMessage(), e);
        }
    }
}
