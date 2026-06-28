package com.dmc.archiving.release.model;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.user.model.User;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
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
@Table(name = "releases", indexes = {
    @Index(name = "idx_dip_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_dip_owner_id", columnList = "owner_id"),
    @Index(name = "idx_dip_status", columnList = "status"),
    @Index(name = "idx_dip_standard", columnList = "standard"),
    @Index(name = "idx_dip_created_at", columnList = "created_at"),
    @Index(name = "idx_dip_updated_at", columnList = "updated_at"),
    @Index(name = "idx_dip_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_dip_owner_status", columnList = "owner_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Release {
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReleaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false)
    private ArchiveStandard standard;

    @Column(name = "source_preservation_id")
    private Long sourcePreservationId;

    /** False when created by an ADMIN operator; such rows are excluded from tenant billing. */
    @Column(name = "billable", nullable = false)
    @ColumnDefault("true")
    private boolean billable = true;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "root_element_id")
    private Element rootElement;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "release_users",
        joinColumns = @JoinColumn(name = "release_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();

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
            status = ReleaseStatus.DRAFT;
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
        Release dip = (Release) o;
        return Objects.equals(id, dip.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Release{" +
                "id=" + id +
                ", ownerId=" + ownerId +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}
