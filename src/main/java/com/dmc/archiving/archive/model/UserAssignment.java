package com.dmc.archiving.archive.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "user_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    private Archive archive;

    // Constructor without archive for convenience
    public UserAssignment(Long userId, UserRole role, LocalDateTime assignedAt) {
        this.userId = userId;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    // GraphQL-compatible getter for assignedAt
    @Transient
    public String getAssignedAtString() {
        return assignedAt != null ? assignedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    // Custom equals and hashCode to avoid circular reference
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAssignment that = (UserAssignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserAssignment{" +
                "id=" + id +
                ", userId=" + userId +
                ", role=" + role +
                ", assignedAt=" + assignedAt +
                '}';
    }
}
