package com.dmc.archiving.archive;

import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.input.AssignUserInput;
import com.dmc.archiving.archive.input.UnassignUserInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    // Existing query methods
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

    // New query methods for user assignments
    @QueryMapping
    public List<Archive> getArchivesByUserAssignment(@Argument Long userId) {
        return archiveService.getArchivesByUserAssignment(userId);
    }

    @QueryMapping
    public List<Archive> getArchivesByUserRole(@Argument Long userId, @Argument UserRole role) {
        return archiveService.getArchivesByUserRole(userId, role);
    }

    // Existing mutation methods
    @MutationMapping
    public Archive createArchive(@Argument CreateArchiveInput input) {
        return archiveService.createArchive(input);
    }

    @MutationMapping
    public Archive updateArchiveStatus(@Argument Long archiveId, @Argument ArchiveStatus status) {
        return archiveService.updateArchiveStatus(archiveId, status);
    }

    @MutationMapping
    public Archive setArchiveRootElement(@Argument Long archiveId, @Argument Long rootElementId) {
        return archiveService.setArchiveRootElement(archiveId, rootElementId);
    }

    // New mutation methods for user assignment
    @MutationMapping
    public Archive assignUserToArchive(@Argument AssignUserInput input) {
        return archiveService.assignUserToArchive(input);
    }

    @MutationMapping
    public Archive unassignUserFromArchive(@Argument UnassignUserInput input) {
        return archiveService.unassignUserFromArchive(input);
    }

    // Field resolvers for datetime-to-string conversion
    @SchemaMapping(typeName = "Archive", field = "createdAt")
    public String createdAt(Archive archive) {
        return archive.getCreatedAt() != null ?
            archive.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @SchemaMapping(typeName = "Archive", field = "updatedAt")
    public String updatedAt(Archive archive) {
        return archive.getUpdatedAt() != null ?
            archive.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    // Field resolver for user assignments
    @SchemaMapping(typeName = "UserAssignment", field = "assignedAt")
    public String assignedAt(com.dmc.archiving.archive.model.UserAssignment userAssignment) {
        return userAssignment.getAssignedAt() != null ?
            userAssignment.getAssignedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
