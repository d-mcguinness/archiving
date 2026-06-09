package com.dmc.archiving.document;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.StorageException;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Raw file upload/download endpoints. Uploads now route through the metered
 * {@link DocumentService} (Review metering-integrity): they create a Document
 * row so the bytes are quota-checked, file-size-capped, and counted toward
 * storage billing — closing the gap where {@code /api/upload(/user)} wrote
 * straight to cloud storage, untracked and unbilled.
 *
 * <p>Identity is derived from the authenticated caller (stashed by
 * {@link RestAuthInterceptor}), never from request params, so the uploader and
 * billing tenant cannot be forged. ADMIN/operator uploads are not billed and
 * carry no tenant; a non-admin caller's single tenant is resolved via
 * {@link BillingTenantResolver}.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:5173", "http://localhost:4173"})
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private final CloudStorageService cloudStorageService;
    private final DocumentService documentService;
    private final BillingTenantResolver billingTenantResolver;
    private final TenancyApi tenancyApi;

    public FileUploadController(CloudStorageService cloudStorageService,
                                DocumentService documentService,
                                BillingTenantResolver billingTenantResolver,
                                TenancyApi tenancyApi) {
        this.cloudStorageService = cloudStorageService;
        this.documentService = documentService;
        this.billingTenantResolver = billingTenantResolver;
        this.tenancyApi = tenancyApi;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        return meteredUpload(file, request);
    }

    @PostMapping("/upload/user")
    public ResponseEntity<?> uploadFileForUser(@RequestParam("file") MultipartFile file,
                                               @RequestParam("userId") Long userId,
                                               HttpServletRequest request) {
        // userId is retained for request-shape compatibility but is NOT trusted
        // for attribution: the uploader and billing tenant come from the token.
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid user ID"));
        }
        return meteredUpload(file, request);
    }

    /** Route an upload through the metered DocumentService and shape a backward-compatible response. */
    private ResponseEntity<?> meteredUpload(MultipartFile file, HttpServletRequest request) {
        AuthContext ctx = caller(request);
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "File is empty"));
        }

        // ADMIN/operator uploads are not billed and carry no tenant (and ADMIN has
        // no single tenant to default to); every other caller's tenant is resolved
        // and the upload is billable.
        Long tenantId;
        try {
            tenantId = ctx.isAdmin() ? null : billingTenantResolver.resolve(ctx, null);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        try {
            Document document = documentService.uploadDocument(
                    file, ctx.userId(), tenantId, null, null, !ctx.isAdmin());

            log.info("File uploaded via metered path: document {} for user {}, tenant {}",
                    document.getId(), ctx.userId(), tenantId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully to cloud storage!");
            response.put("documentId", document.getId());
            response.put("fileKey", document.getFileKey());
            response.put("fileUrl", document.getFileUrl());
            response.put("originalFilename", document.getFileName());
            response.put("size", document.getFileSize());
            response.put("contentType", document.getContentType());
            return ResponseEntity.ok(response);

        } catch (FileTooLargeException e) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (StorageQuotaExceededException e) {
            return ResponseEntity.status(HttpStatus.INSUFFICIENT_STORAGE)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (SpendCapExceededException e) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileKey}")
    public ResponseEntity<?> downloadFile(@PathVariable String fileKey, HttpServletRequest request) {
        // Tenant-ownership: only presign keys that map to a tracked document the
        // caller may access. Without this, any authenticated user could download
        // any object by its (guessable) key — a cross-tenant IDOR.
        Document document = documentService.getDocumentByFileKey(fileKey).orElse(null);
        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "File not found"));
        }
        if (!canAccess(caller(request), document.getTenantId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "Access denied: not a member of the file's tenant"));
        }

        try {
            String presignedUrl = cloudStorageService.getPresignedUrl(fileKey, 60);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("downloadUrl", presignedUrl);
            response.put("message", "Presigned URL generated successfully");

            return ResponseEntity.ok(response);

        } catch (StorageException e) {
            log.error("Failed to generate download URL: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Failed to generate download URL"));
        }
    }

    @GetMapping("/upload/info")
    public ResponseEntity<?> getUploadInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("storageType", "AWS S3 Cloud Storage");
        info.put("maxFileSize", 50 * 1024 * 1024);
        info.put("maxFileSizeMB", 50);
        info.put("supportedOperations", new String[]{
            "upload", "download", "delete", "presigned-urls"
        });

        return ResponseEntity.ok(info);
    }

    /** The authenticated caller, stashed by RestAuthInterceptor (auth is enforced there). */
    private AuthContext caller(HttpServletRequest request) {
        AuthContext ctx = (AuthContext) request.getAttribute(RestAuthInterceptor.AUTH_CONTEXT);
        return ctx != null ? ctx : AuthContext.ANONYMOUS;
    }

    /** ADMIN may access any tenant's files; others must be a member of the file's tenant. */
    private boolean canAccess(AuthContext ctx, Long tenantId) {
        return ctx.isAdmin() || (tenantId != null && tenancyApi.isUserInTenant(ctx.userId(), tenantId));
    }
}
