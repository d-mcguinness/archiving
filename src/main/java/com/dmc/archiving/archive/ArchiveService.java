package com.dmc.archiving.archive;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.ElementRepository;
import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.input.UpdateArchiveInput;
import com.dmc.archiving.archive.input.AssignUserInput;
import com.dmc.archiving.archive.input.UnassignUserInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.archive.specification.ArchiveSearchCriteria;
import com.dmc.archiving.archive.specification.ArchiveSpecifications;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing archives
 * All query methods are marked @Transactional(readOnly = true) for performance
 * All write methods are marked @Transactional for ACID guarantees
 * Caching is enabled for frequently accessed data
 */
@Service
@Transactional(readOnly = true)
@CacheConfig(cacheNames = "archives")
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final ArchiveRepository archiveRepository;
    private final UserApi userApi;
    private final ElementRepository elementRepository;

    public ArchiveService(ArchiveRepository archiveRepository, UserApi userApi, ElementRepository elementRepository) {
        this.archiveRepository = archiveRepository;
        this.userApi = userApi;
        this.elementRepository = elementRepository;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "archives", allEntries = true),
            @CacheEvict(cacheNames = "archivesByTenant", allEntries = true),
            @CacheEvict(cacheNames = "archivesByOwner", allEntries = true)
    })
    @Transactional
    public Archive createArchive(CreateArchiveInput input) {
        log.info("Creating archive with title: {} for user: {}", input.getTitle(), input.getUserId());

        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", input.getUserId()));

        LocalDateTime now = LocalDateTime.now();

        // Use ownerId if provided, otherwise fall back to userId
        Long ownerId = input.getOwnerId() != null ? input.getOwnerId() : input.getUserId();
        // Use tenantId if provided, otherwise default to ownerId as a placeholder
        Long tenantId = input.getTenantId() != null ? input.getTenantId() : ownerId;

        Archive archive = new Archive(
            null, // Let JPA generate the ID
            tenantId,             // Set tenantId (organization)
            ownerId,              // Set ownerId (user who owns it)
            input.getTitle(),
            input.getDescription(),
            input.getContent(),
            now,
            now,
            ArchiveStatus.DRAFT,
            input.getStandard()
        );

        // Automatically assign the creator
        archive.assignUser(user);

        return archiveRepository.save(archive);
    }

    @Transactional
    public Archive assignUserToArchive(AssignUserInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Archive archive = archiveRepository.findById(input.getArchiveId())
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + input.getArchiveId() + " does not exist"));

        archive.assignUser(user);
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    @Transactional
    public Archive unassignUserFromArchive(UnassignUserInput input) {
        User user = userApi.getUserById(input.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist"));

        Archive archive = archiveRepository.findById(input.getArchiveId())
                .orElseThrow(() -> new IllegalArgumentException("Archive with ID " + input.getArchiveId() + " does not exist"));

        // Don't allow unassigning the owner
        if (archive.getOwnerId().equals(input.getUserId())) {
            throw new IllegalArgumentException("Cannot unassign the owner from the archive");
        }

        archive.unassignUser(user);
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    public List<Archive> getArchivesByUserAssignment(Long userId) {
        return archiveRepository.findArchivesByUserIdOwnerOrAssigned(userId);
    }

    public List<Archive> getArchivesByOwner(Long ownerId) {
        return archiveRepository.findByOwnerId(ownerId);
    }

    public List<Archive> getAllSips() {
        return archiveRepository.findByRootElementIsNotNull();
    }

    public List<Archive> getSipsByTenant(Long tenantId) {
        return archiveRepository.findSipsByTenantId(tenantId);
    }

    @Cacheable(key = "#tenantId", cacheNames = "archivesByTenant")
    public List<Archive> getArchivesByTenant(Long tenantId) {
        return archiveRepository.findByTenantId(tenantId);
    }

    @Cacheable(key = "#id")
    public Archive getArchiveById(Long id) {
        return archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive", id));
    }

    @Cacheable(key = "#id", cacheNames = "archiveWithRelations")
    public Archive getArchiveByIdWithRelations(Long id) {
        return archiveRepository.findByIdWithRelations(id);
    }

    /**
     * Get archive with fully initialized element tree for export.
     * Uses @Transactional to keep the session open for lazy loading.
     */
    @Transactional(readOnly = true)
    public Archive getArchiveForExport(Long id) {
        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive", id));

        // Initialize the element tree by traversing it
        if (archive.getRootElement() != null) {
            initializeElementTree(archive.getRootElement());
        }

        // Also initialize the flat elements set (fallback for archives without rootElement)
        if (archive.getElements() != null) {
            archive.getElements().size(); // force init
            for (Element element : archive.getElements()) {
                initializeElementTree(element);
            }
        }

        // Initialize assigned users
        if (archive.getAssignedUsers() != null) {
            archive.getAssignedUsers().size();
        }

        return archive;
    }

    private void initializeElementTree(Element element) {
        // Force initialization of lazy collections
        if (element.getFields() != null) {
            element.getFields().size();
        }
        if (element.getOutgoingLinks() != null) {
            element.getOutgoingLinks().size();
        }
        if (element.getChildren() != null) {
            element.getChildren().size();
            for (Element child : element.getChildren()) {
                initializeElementTree(child);
            }
        }
    }

    // ========== Paginated Query Methods (Recommended for scalability) ==========

    public Page<Archive> getAllArchivesPaginated(Pageable pageable) {
        return archiveRepository.findAll(pageable);
    }

    /**
     * Advanced search using dynamic specifications
     * Allows combining multiple filter criteria
     */
    public Page<Archive> searchArchives(ArchiveSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching archives with criteria: {}", criteria);

        Specification<Archive> spec = Specification.where(
                ArchiveSpecifications.hasTenantId(criteria.getTenantId()))
                .and(ArchiveSpecifications.hasOwnerId(criteria.getOwnerId()))
                .and(ArchiveSpecifications.hasStatus(criteria.getStatus()))
                .and(ArchiveSpecifications.hasStandard(criteria.getStandard()))
                .and(ArchiveSpecifications.searchByKeyword(criteria.getKeyword()))
                .and(ArchiveSpecifications.createdAfter(criteria.getFromDate()))
                .and(ArchiveSpecifications.createdBefore(criteria.getToDate()))
                .and(ArchiveSpecifications.updatedAfter(criteria.getUpdatedAfter()));

        // Handle array filters
        if (criteria.getStatuses() != null && criteria.getStatuses().length > 0) {
            spec = spec.and(ArchiveSpecifications.hasStatusIn(criteria.getStatuses()));
        }

        if (criteria.getStandards() != null && criteria.getStandards().length > 0) {
            spec = spec.and(ArchiveSpecifications.hasStandardIn(criteria.getStandards()));
        }

        return archiveRepository.findAll(spec, pageable);
    }

    public Page<Archive> getArchivesByUserIdPaginated(Long userId, Pageable pageable) {
        return archiveRepository.findByOwnerId(userId, pageable);
    }

    public Page<Archive> getArchivesByUserAssignmentPaginated(Long userId, Pageable pageable) {
        return archiveRepository.findArchivesByUserIdOwnerOrAssigned(userId, pageable);
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

    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#id", cacheNames = "archiveWithRelations"),
            @CacheEvict(cacheNames = "archivesByTenant", allEntries = true),
            @CacheEvict(cacheNames = "archivesByOwner", allEntries = true)
    })
    @Transactional
    public Archive updateArchiveStatus(Long archiveId, ArchiveStatus status) {
        log.info("Updating archive {} status to {}", archiveId, status);

        Archive archive = archiveRepository.findById(archiveId)
                .orElseThrow(() -> new ResourceNotFoundException("Archive", archiveId));

        archive.setStatus(status);
        archive.setUpdatedAt(LocalDateTime.now());
        return archiveRepository.save(archive);
    }

    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#id", cacheNames = "archiveWithRelations"),
            @CacheEvict(cacheNames = "archivesByTenant", allEntries = true),
            @CacheEvict(cacheNames = "archivesByOwner", allEntries = true)
    })
    @Transactional
    public Archive updateArchive(Long id, UpdateArchiveInput input) {
        log.info("Updating archive {}", id);

        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive", id));

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
                .orElseThrow(() -> new ResourceNotFoundException("Archive", archiveId));

        Element rootElement = elementRepository.findById(rootElementId)
                .orElseThrow(() -> new ResourceNotFoundException("Element", rootElementId));

        // Verify the element belongs to this archive
        if (!rootElement.getArchive().getId().equals(archiveId)) {
            throw new IllegalArgumentException("Element does not belong to this archive");
        }

        // Set the root element
        archive.setRootElement(rootElement);
        archive.setUpdatedAt(LocalDateTime.now());

        return archiveRepository.save(archive);
    }

    @Caching(evict = {
            @CacheEvict(key = "#id"),
            @CacheEvict(key = "#id", cacheNames = "archiveWithRelations"),
            @CacheEvict(cacheNames = "archives", allEntries = true),
            @CacheEvict(cacheNames = "archivesByTenant", allEntries = true),
            @CacheEvict(cacheNames = "archivesByOwner", allEntries = true)
    })
    @Transactional
    public Boolean deleteArchive(Long id) {
        log.warn("Deleting archive {}", id);

        Archive archive = archiveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Archive", id));

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

            // Assigned users
            if (archive.getAssignedUsers() != null && !archive.getAssignedUsers().isEmpty()) {
                List<Map<String, Object>> users = archive.getAssignedUsers().stream()
                    .map(user -> {
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("id", user.getId());
                        userData.put("name", user.getName());
                        userData.put("email", user.getEmail());
                        return userData;
                    })
                    .collect(Collectors.toList());
                exportData.put("assignedUsers", users);
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
