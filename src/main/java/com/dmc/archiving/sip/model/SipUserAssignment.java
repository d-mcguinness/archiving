package com.dmc.archiving.sip.model;

import com.dmc.archiving.archive.model.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
@Table(name = "sip_user_assignments", indexes = {
    @Index(name = "idx_sua_user_id", columnList = "user_id"),
    @Index(name = "idx_sua_sip_id", columnList = "sip_id"),
    @Index(name = "idx_sua_user_sip", columnList = "user_id, sip_id", unique = true),
    @Index(name = "idx_sua_role", columnList = "role"),
    @Index(name = "idx_sua_assigned_at", columnList = "assigned_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SipUserAssignment {
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
    @JoinColumn(name = "sip_id", nullable = false)
    private Sip sip;

    public SipUserAssignment(Long userId, UserRole role, LocalDateTime assignedAt) {
        this.userId = userId;
        this.role = role;
        this.assignedAt = assignedAt;
    }

    @Transient
    public String getAssignedAtString() {
        return assignedAt != null ? assignedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SipUserAssignment that = (SipUserAssignment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SipUserAssignment{" +
                "id=" + id +
                ", userId=" + userId +
                ", role=" + role +
                ", assignedAt=" + assignedAt +
                '}';
    }
}
