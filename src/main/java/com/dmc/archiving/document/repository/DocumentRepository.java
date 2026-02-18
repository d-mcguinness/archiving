package com.dmc.archiving.document.repository;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find by user
    List<Document> findByUserId(Long userId);

    List<Document> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Find by tenant
    List<Document> findByTenantId(Long tenantId);

    List<Document> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    // Find by archive
    List<Document> findByArchiveId(Long archiveId);

    // Find by status
    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status);

    // Find by user and status
    List<Document> findByUserIdAndStatus(Long userId, DocumentStatus status);

    // Find by tenant and status
    List<Document> findByTenantIdAndStatus(Long tenantId, DocumentStatus status);

    // Get all documents ordered by creation date
    List<Document> findAllByOrderByCreatedAtDesc();

    // Find by file key
    Document findByFileKey(String fileKey);

    // Count documents by user
    long countByUserId(Long userId);

    // Count documents by tenant
    long countByTenantId(Long tenantId);
}

