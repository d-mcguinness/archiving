package com.dmc.archiving.pkg.model;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Unified entity for SIP, AIP, and DIP.
 * The `stage` field determines which lifecycle phase the package represents.
 */
@Entity
@Table(name = "packages", indexes = {
    @Index(name = "idx_pkg_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_pkg_owner_id", columnList = "owner_id"),
    @Index(name = "idx_pkg_stage", columnList = "stage"),
    @Index(name = "idx_pkg_status", columnList = "status"),
    @Index(name = "idx_pkg_standard", columnList = "standard"),
    @Index(name = "idx_pkg_created_at", columnList = "created_at"),
    @Index(name = "idx_pkg_tenant_stage", columnList = "tenant_id, stage"),
    @Index(name = "idx_pkg_owner_status", columnList = "owner_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArchivalPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 10)
    private PackageStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PackageStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false)
    private ArchiveStandard standard;

    /** For SIPs created from an archive */
    @Column(name = "source_archive_id")
    private Long sourceArchiveId;

    /** Self-referential: AIP points to source SIP package, DIP points to source AIP package */
    @Column(name = "source_package_id")
    private Long sourcePackageId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "root_element_id")
    private Element rootElement;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "package_users",
        joinColumns = @JoinColumn(name = "package_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    public String getCreatedAtString() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Transient
    public String getUpdatedAtString() {
        return updatedAt != null ? updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    public void assignUser(User user) {
        if (assignedUsers == null) {
            assignedUsers = new HashSet<>();
        }
        assignedUsers.add(user);
    }

    public void setRootElement(Element element) {
        this.rootElement = element;
        if (element != null) {
            element.setIsRoot(true);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PackageStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchivalPackage that = (ArchivalPackage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ArchivalPackage{" +
                "id=" + id +
                ", stage=" + stage +
                ", ownerId=" + ownerId +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}
