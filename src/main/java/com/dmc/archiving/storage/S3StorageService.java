package com.dmc.archiving.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AWS S3 implementation of CloudStorageService
 */
@Service
public class S3StorageService implements CloudStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3StorageService(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey,
            @Value("${aws.use-localstack:false}") boolean useLocalStack,
            @Value("${aws.localstack.endpoint:http://localhost:4566}") String localStackEndpoint) {

        this.bucketName = bucketName;

        // Create AWS credentials
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        // Build S3 client with LocalStack support
        if (useLocalStack) {
            log.info("Initializing S3 Storage Service with LocalStack endpoint: {}", localStackEndpoint);

            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(java.net.URI.create(localStackEndpoint))
                    .forcePathStyle(true)  // Required for LocalStack
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(java.net.URI.create(localStackEndpoint))
                    .build();
        } else {
            log.info("Initializing S3 Storage Service with AWS S3 in region: {}", region);

            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();

            this.s3Presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(credentialsProvider)
                    .build();
        }

        log.info("S3 Storage Service initialized with bucket: {} in region: {} (LocalStack: {})",
                bucketName, region, useLocalStack);

        // Create bucket if using LocalStack and bucket doesn't exist
        if (useLocalStack) {
            createBucketIfNotExists();
        }
    }

    @Override
    public UploadResult uploadFile(MultipartFile file, Long userId) throws StorageException {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new StorageException("Cannot upload empty file");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new StorageException("File size exceeds maximum limit of 50MB");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new StorageException("Invalid filename");
            }

            // Sanitize filename
            String sanitizedFilename = sanitizeFilename(originalFilename);

            // Generate unique file key (path in S3)
            String fileKey = generateFileKey(sanitizedFilename, userId);

            // Prepare metadata
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "original-filename", originalFilename,
                            "upload-time", LocalDateTime.now().toString(),
                            "user-id", userId != null ? userId.toString() : "unknown"
                    ))
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("File uploaded to S3: {} (original: {})", fileKey, originalFilename);

            // Generate presigned URL valid for 7 days
            String presignedUrl = getPresignedUrl(fileKey, 60 * 24 * 7);

            return UploadResult.builder()
                    .fileKey(fileKey)
                    .fileUrl(presignedUrl)
                    .originalFilename(originalFilename)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .uploadTime(LocalDateTime.now().toString())
                    .userId(userId)
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new StorageException("Failed to upload file to S3", e);
        } catch (S3Exception e) {
            log.error("S3 error during upload: {}", e.getMessage(), e);
            throw new StorageException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public InputStream downloadFile(String fileKey) throws StorageException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            return s3Client.getObject(getObjectRequest);

        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: {}", fileKey);
            throw new StorageException("File not found: " + fileKey, e);
        } catch (S3Exception e) {
            log.error("S3 error during download: {}", e.getMessage(), e);
            throw new StorageException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileKey) throws StorageException {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted from S3: {}", fileKey);

        } catch (S3Exception e) {
            log.error("S3 error during delete: {}", e.getMessage(), e);
            throw new StorageException("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean fileExists(String fileKey) throws StorageException {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("S3 error checking file existence: {}", e.getMessage(), e);
            throw new StorageException("Failed to check file existence", e);
        }
    }

    @Override
    public String getPresignedUrl(String fileKey, int expirationMinutes) throws StorageException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();

        } catch (S3Exception e) {
            log.error("S3 error generating presigned URL: {}", e.getMessage(), e);
            throw new StorageException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public String uploadBytes(byte[] data, String key, String contentType) throws StorageException {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) data.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(data));
            log.info("Bytes uploaded to S3: {} ({} bytes)", key, data.length);

            return getPresignedUrl(key, 60);
        } catch (S3Exception e) {
            log.error("S3 error during byte upload: {}", e.getMessage(), e);
            throw new StorageException("Failed to upload bytes to S3", e);
        }
    }

    /**
     * Generate a unique file key (path) in S3
     */
    private String generateFileKey(String filename, Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueFilename = timestamp + "_" + filename;

        if (userId != null) {
            return "users/" + userId + "/" + uniqueFilename;
        } else {
            return "uploads/" + uniqueFilename;
        }
    }

    /**
     * Sanitize filename to prevent security issues
     */
    private String sanitizeFilename(String filename) {
        // Remove path separators and null bytes
        String sanitized = filename.replaceAll("[/\\\\\\x00]", "_");

        // Remove leading dots to prevent hidden files
        sanitized = sanitized.replaceAll("^\\.+", "");

        // If filename becomes empty after sanitization, use a default
        if (sanitized.isEmpty()) {
            sanitized = "uploaded_file";
        }

        return sanitized;
    }

    /**
     * Create S3 bucket if it doesn't exist (for LocalStack)
     */
    private void createBucketIfNotExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            log.info("LocalStack bucket '{}' already exists", bucketName);
        } catch (Exception e) {
            // Bucket doesn't exist, create it
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();

                s3Client.createBucket(createBucketRequest);
                log.info("LocalStack bucket '{}' created successfully", bucketName);
            } catch (Exception createException) {
                log.error("Failed to create LocalStack bucket '{}': {}", bucketName, createException.getMessage());
            }
        }
    }
}

