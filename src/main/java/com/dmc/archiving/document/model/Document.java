package com.dmc.archiving.document.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.modulith.NamedInterface;

import java.time.LocalDateTime;

/**
 * Document entity - represents uploaded documents in the system
 * Documents can be associated with archives and users
 */
@NamedInterface
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_user_id", columnList = "user_id"),
    @Index(name = "idx_document_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_document_archive_id", columnList = "archive_id"),
    @Index(name = "idx_document_sip_id", columnList = "sip_id"),
    @Index(name = "idx_document_created_at", columnList = "created_at"),
    @Index(name = "idx_document_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_key", nullable = false, length = 1000)
    private String fileKey;  // S3/LocalStack file key

    @Column(name = "file_url", length = 2000)
    private String fileUrl;  // Presigned URL or access URL

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // Owner/uploader of the document

    @Column(name = "tenant_id")
    private Long tenantId;  // Tenant the document belongs to

    @Column(name = "archive_id")
    private Long archiveId;  // Optional: associated archive

    @Column(name = "sip_id")
    private Long sipId;  // Optional: associated SIP

    /** False when uploaded by an ADMIN operator; such documents are excluded from tenant billing. */
    @Column(name = "billable", nullable = false)
    @ColumnDefault("true")
    private boolean billable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        uploadedAt = LocalDateTime.now();
        if (status == null) {
            status = DocumentStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

