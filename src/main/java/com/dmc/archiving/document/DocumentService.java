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
        return uploadDocument(file, userId, tenantId, title, description, true);
    }

    /** Per-file cap when the tenant/plan is unknown. */
    private static final long DEFAULT_MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

    @Transactional
    public Document uploadDocument(MultipartFile file, Long userId, Long tenantId, String title,
                                   String description, boolean billable) {
        // Per-file size cap for the tenant's plan (a technical limit; applies to
        // every upload, billable or not).
        enforceFileSizeLimit(tenantId, file.getSize());
        // Enforce the tenant's storage allotment before writing anything to S3.
        // Only billable uploads count against the meter; operator (non-billable)
        // uploads bypass the quota, consistent with not being billed.
        if (billable && tenantId != null) {
            enforceStorageQuota(tenantId, file.getSize());
        }
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
            document.setBillable(billable);

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
     * Get document by its storage file key (for tenant-ownership checks on raw
     * presigned-URL downloads). Empty when the key maps to no tracked document.
     */
    public Optional<Document> getDocumentByFileKey(String fileKey) {
        return Optional.ofNullable(documentRepository.findByFileKey(fileKey));
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
    public List<Document> getDocumentsByIntake(Long intakeId) {
        return documentRepository.findByIntakeId(intakeId);
    }

    /**
     * Associate document with SIP
     */
    @Transactional
    public Document associateWithIntake(Long documentId, Long intakeId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        document.setIntakeId(intakeId);
        return documentRepository.save(document);
    }

    /**
     * Disassociate document from SIP
     */
    @Transactional
    public Document disassociateFromIntake(Long documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + documentId));

        document.setIntakeId(null);
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

    /**
     * Reject an upload whose single-file size exceeds the tenant plan's cap.
     * ENTERPRISE/CUSTOM get a raised large-file ceiling; others get the default.
     */
    private void enforceFileSizeLimit(Long tenantId, long size) {
        long max = tenantId != null
                ? tenancyApi.getMaxUploadFileSizeBytes(tenantId)
                : DEFAULT_MAX_UPLOAD_BYTES;
        if (size > max) {
            throw new FileTooLargeException(
                    "File of " + size + " bytes exceeds the per-file limit of " + max
                            + " bytes for this plan. Upgrade to a plan with the large-file add-on.");
        }
    }

    /**
     * Reject (FREE) or allow-with-overage (paid) a billable upload against the
     * tenant's storage allotment. -1 limit means unlimited.
     */
    private void enforceStorageQuota(Long tenantId, long incomingBytes) {
        long limit = tenancyApi.getStorageLimitBytes(tenantId);
        if (limit < 0) {
            return; // unlimited
        }
        // Serialize concurrent billable uploads for this tenant so the usage
        // read and the subsequent write cannot both pass the cap.
        tenancyApi.lockTenantForUpdate(tenantId);
        long current = documentRepository.sumFileSizeByTenantId(tenantId);
        long projected = current + incomingBytes;
        if (projected <= limit) {
            return; // within allotment
        }
        // Over the plan allotment.
        if (!tenancyApi.isOverageAllowed(tenantId)) {
            // FREE: hard stop at the allotment.
            throw new StorageQuotaExceededException(
                    "Storage limit exceeded for tenant " + tenantId + ": "
                            + projected + " bytes would exceed the plan allotment of " + limit
                            + " bytes. Upgrade the plan or free up space.");
        }
        // Paid: allowed to incur overage up to the configured spend cap.
        long overage = projected - limit;
        long budget = tenancyApi.getStorageOverageLimitBytes(tenantId);
        if (budget < 0) {
            log.warn("Tenant {} over storage allotment by {} bytes (unlimited overage); recording billable overage",
                    tenantId, overage);
            return;
        }
        if (overage > budget && !tenancyApi.isOverageOptIn(tenantId)) {
            throw new SpendCapExceededException(
                    "Overage spend cap reached for tenant " + tenantId + ": projected overage of "
                            + overage + " bytes exceeds the cap of " + budget
                            + " bytes. Raise the cap, opt in to keep accruing, or free up space.");
        }
        alertOverage(tenantId, overage, budget);
    }

    /** Emit a threshold alert as the tenant consumes its overage budget. */
    private void alertOverage(Long tenantId, long overage, long budget) {
        if (overage > budget) {
            log.warn("Tenant {} ACCRUING PAST overage cap (opted in): {} / {} bytes", tenantId, overage, budget);
            return;
        }
        long pct = budget == 0 ? 100 : (overage * 100 / budget);
        int band = pct >= 100 ? 100 : pct >= 80 ? 80 : pct >= 50 ? 50 : 0;
        if (band > 0) {
            log.warn("Tenant {} storage overage at {}% of spend cap ({} / {} bytes)",
                    tenantId, band, overage, budget);
        }
    }
}

