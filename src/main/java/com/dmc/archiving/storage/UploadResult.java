package com.dmc.archiving.storage;

import lombok.Builder;
import lombok.Data;

/**
 * Result object returned after file upload
 */
@Data
@Builder
public class UploadResult {
    private String fileKey;           // Unique identifier/path in cloud storage
    private String fileUrl;           // Public or presigned URL to access the file
    private String originalFilename;  // Original filename from upload
    private String contentType;       // MIME type
    private long fileSize;           // Size in bytes
    private String uploadTime;       // Timestamp of upload
    private Long userId;             // Associated user ID (optional)
}

