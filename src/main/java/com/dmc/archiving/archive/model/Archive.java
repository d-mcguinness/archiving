package com.dmc.archiving.archive.model;

import com.dmc.archiving.archive.element.Element;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.HashSet;
import java.util.Objects;

@Entity
@Table(name = "archives", indexes = {
    @Index(name = "idx_archive_owner_id", columnList = "owner_id"),
    @Index(name = "idx_archive_status", columnList = "status"),
    @Index(name = "idx_archive_standard", columnList = "standard"),
    @Index(name = "idx_archive_created_at", columnList = "created_at"),
    @Index(name = "idx_archive_updated_at", columnList = "updated_at"),
    @Index(name = "idx_archive_owner_status", columnList = "owner_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Archive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;  // Primary owner of the archive

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ArchiveStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false)
    private ArchiveStandard standard;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "root_element_id")
    private Element rootElement;

    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Element> elements = new HashSet<>();

    @OneToMany(mappedBy = "archive", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserAssignment> assignedUsers = new HashSet<>();

    // Constructor for backward compatibility (without assignments)
    public Archive(Long id, Long ownerId, String title, String description,
                   String content, LocalDateTime createdAt, LocalDateTime updatedAt,
                   ArchiveStatus status, ArchiveStandard standard) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.standard = standard;
        this.elements = new HashSet<>();
        this.assignedUsers = new HashSet<>();
    }

    // GraphQL-compatible getters that return String representations
    @Transient
    public String getCreatedAtString() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Transient
    public String getUpdatedAtString() {
        return updatedAt != null ? updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    // Helper methods for user assignment management
    public void assignUser(Long userId, UserRole role) {
        if (assignedUsers == null) {
            assignedUsers = new HashSet<>();
        }
        UserAssignment assignment = new UserAssignment();
        assignment.setUserId(userId);
        assignment.setRole(role);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setArchive(this);
        assignedUsers.add(assignment);
    }

    public void unassignUser(Long userId) {
        if (assignedUsers != null) {
            assignedUsers.removeIf(assignment -> assignment.getUserId().equals(userId));
        }
    }

    public boolean isUserAssigned(Long userId) {
        return assignedUsers != null &&
               assignedUsers.stream().anyMatch(assignment -> assignment.getUserId().equals(userId));
    }

    public UserRole getUserRole(Long userId) {
        if (assignedUsers == null) return null;
        return assignedUsers.stream()
                .filter(assignment -> assignment.getUserId().equals(userId))
                .map(UserAssignment::getRole)
                .findFirst()
                .orElse(null);
    }

    // Helper method for root element management
    public void setRootElement(Element element) {
        this.rootElement = element;
        if (element != null) {
            element.setArchive(this);
            element.setIsRoot(true);
        }
    }

    // Custom equals and hashCode to avoid circular reference
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Archive archive = (Archive) o;
        return Objects.equals(id, archive.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Archive{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}
