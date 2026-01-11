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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArchiveService {

    private final ArchiveRepository archiveRepository;
    private final UserApi userApi;
    private final ElementRepository elementRepository;

    public ArchiveService(ArchiveRepository archiveRepository, UserApi userApi, ElementRepository elementRepository) {
        this.archiveRepository = archiveRepository;
        this.userApi = userApi;
        this.elementRepository = elementRepository;
    }

    public Archive createArchive(CreateArchiveInput input) {
        // Validate that user exists using the public API
        if (!userApi.userExists(input.getUserId())) {
            throw new IllegalArgumentException("User with ID " + input.getUserId() + " does not exist");
        }

        LocalDateTime now = LocalDateTime.now();

        Archive archive = new Archive(
            null, // Let JPA generate the ID
            input.getUserId(),  // Set as ownerId
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

    public List<Archive> getAllArchives() {
        return archiveRepository.findAll();
    }

    public List<Archive> getArchivesByUserId(Long userId) {
        // Return archives where user is owner (for backward compatibility)
        return archiveRepository.findByOwnerId(userId);
    }

    public Archive updateArchiveStatus(Long archiveId, ArchiveStatus status) {
        Archive archive = archiveRepository.findById(archiveId).orElse(null);
        if (archive != null) {
            archive.setStatus(status);
            archive.setUpdatedAt(LocalDateTime.now());
            return archiveRepository.save(archive);
        }
        return null;
    }

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
}
