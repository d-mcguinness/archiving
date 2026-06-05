package com.dmc.archiving.document;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import com.dmc.archiving.document.repository.DocumentRepository;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.UploadResult;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final CloudStorageService cloudStorageService;
    private final TenancyApi tenancyApi;

    public DocumentService(DocumentRepository documentRepository,
                          CloudStorageService cloudStorageService,
                          TenancyApi tenancyApi) {
        this.documentRepository = documentRepository;
        this.cloudStorageService = cloudStorageService;
        this.tenancyApi = tenancyApi;
    }

    /**
     * Upload a document for a user
     */
    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId, Long tenantId, String title, String description) {
        try {
            // Upload file to cloud storage
            UploadResult uploadResult = cloudStorageService.uploadFile(file, userId);

            // Create document entity
            Document document = new Document();
            document.setTitle(title != null ? title : file.getOriginalFilename());
            document.setDescription(description);
            document.setFileName(uploadResult.getOriginalFilename());
            document.setFileKey(uploadResult.getFileKey());
            document.setFileUrl(uploadResult.getFileUrl());
            document.setFileSize(uploadResult.getFileSize());
            document.setContentType(uploadResult.getContentType());
            document.setUserId(userId);
            document.setTenantId(tenantId);
            document.setStatus(DocumentStatus.ACTIVE);

            return documentRepository.save(document);
        } catch (Exception e) {
            log.error("Error uploading document for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    public Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    /**
     * Get all documents (ADMIN only)
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get documents by user
     */
    public List<Document> getDocumentsByUser(Long userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get documents by user and tenant
     */
    public List<Document> getDocumentsByUserAndTenant(Long userId, Long tenantId) {
        return documentRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId);
    }

    /**
     * Get documents by tenant
     */
    public List<Document> getDocumentsByTenant(Long tenantId) {
        return documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Get documents for all tenants that a user belongs to
     * Uses user_tenant association to find user's tenants
     */
    public List<Document> getDocumentsByUserTenants(Long userId) {
        // Get all tenant IDs the user belongs to (using user_tenant table)
        List<Long> tenantIds = tenancyApi.getTenantIdsByUserId(userId);

        if (tenantIds.isEmpty()) {
            log.warn("User {} does not belong to any tenants", userId);
            return List.of();
        }

        log.info("Fetching documents for user {} from tenants: {}", userId, tenantIds);

        // Get all documents from these tenants
        return tenantIds.stream()
            .flatMap(tenantId -> documentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream())
            .distinct()
            .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
            .collect(Collectors.toList());
    }

    /**
     * Get documents by archive
     */
    public List<Document> getDocumentsByArchive(Long archiveId) {
        return documentRepository.findByArchiveId(archiveId);
    }

    /**
     * Get document by ID
     */
    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    /**
     * Update document
     */
    @Transactional
    public Document updateDocument(Long id, String title, String description, DocumentStatus status) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        if (title != null) {
            document.setTitle(title);
        }
        if (description != null) {
            document.setDescription(description);
        }
        if (status != null) {
            document.setStatus(status);
        }

        return documentRepository.save(document);
    }

    /**
     * Get documents by SIP
     */
    public List<Document> getDocumentsBySip(Long sipId) {
        return documentRepository.findBySipId(sipId);
    }

    /**
     * Associate document with SIP
     */
    @Transactional
    public Document associateWithSip(Long documentId, Long sipId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        document.setSipId(sipId);
        return documentRepository.save(document);
    }

    /**
     * Disassociate document from SIP
     */
    @Transactional
    public Document disassociateFromSip(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        document.setSipId(null);
        return documentRepository.save(document);
    }

    /**
     * Associate document with archive
     */
    @Transactional
    public Document associateWithArchive(Long documentId, Long archiveId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        document.setArchiveId(archiveId);
        return documentRepository.save(document);
    }

    /**
     * Delete document
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        Optional<Document> documentOpt = documentRepository.findById(id);
        if (documentOpt.isEmpty()) {
            return false;
        }

        Document document = documentOpt.get();

        try {
            // Delete from cloud storage
            cloudStorageService.deleteFile(document.getFileKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from storage: {}", e.getMessage());
            // Continue with database deletion even if storage deletion fails
        }

        // Delete from database
        documentRepository.delete(document);
        return true;
    }

    /**
     * Get download URL for document
     */
    public String getDownloadUrl(Long id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + id));

        try {
            return cloudStorageService.getPresignedUrl(document.getFileKey(), 60); // 1 hour expiry
        } catch (Exception e) {
            log.error("Error generating download URL for document {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    /**
     * Count documents by user
     */
    public long countByUser(Long userId) {
        return documentRepository.countByUserId(userId);
    }

    /**
     * Count documents by tenant
     */
    public long countByTenant(Long tenantId) {
        return documentRepository.countByTenantId(tenantId);
    }

    /**
     * Total stored bytes for a tenant (SQL aggregate, for usage metering).
     */
    public long getStorageBytesByTenant(Long tenantId) {
        return documentRepository.sumFileSizeByTenantId(tenantId);
    }
}

