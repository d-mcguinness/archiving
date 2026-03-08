package com.dmc.archiving.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Interface for cloud storage operations
 * Supports multiple cloud providers (AWS S3, Google Cloud Storage, Azure Blob Storage)
 */
public interface CloudStorageService {

    /**
     * Upload a file to cloud storage
     *
     * @param file The file to upload
     * @param userId Optional user ID for organizing files
     * @return UploadResult containing file URL and metadata
     */
    UploadResult uploadFile(MultipartFile file, Long userId) throws StorageException;

    /**
     * Download a file from cloud storage
     *
     * @param fileKey The unique key/path of the file
     * @return InputStream of the file
     */
    InputStream downloadFile(String fileKey) throws StorageException;

    /**
     * Delete a file from cloud storage
     *
     * @param fileKey The unique key/path of the file
     */
    void deleteFile(String fileKey) throws StorageException;

    /**
     * Check if a file exists in cloud storage
     *
     * @param fileKey The unique key/path of the file
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String fileKey) throws StorageException;

    /**
     * Get a presigned URL for direct file access
     *
     * @param fileKey The unique key/path of the file
     * @param expirationMinutes How long the URL should be valid (in minutes)
     * @return Presigned URL
     */
    String getPresignedUrl(String fileKey, int expirationMinutes) throws StorageException;

    /**
     * Upload raw bytes to cloud storage
     *
     * @param data The byte array to upload
     * @param key The storage key/path for the file
     * @param contentType The MIME content type
     * @return Presigned URL for accessing the uploaded file
     */
    String uploadBytes(byte[] data, String key, String contentType) throws StorageException;
}

