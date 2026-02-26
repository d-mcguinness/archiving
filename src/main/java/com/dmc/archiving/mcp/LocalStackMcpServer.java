package com.dmc.archiving.mcp;

import com.dmc.archiving.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Server for LocalStack S3 Operations
 * Provides tools for AI assistants to interact with LocalStack S3
 * Only active in local profile
 */
@RestController
@RequestMapping("/mcp/localstack")
@Profile("local")
@ConditionalOnProperty(name = "aws.use-localstack", havingValue = "true")
public class LocalStackMcpServer {

    private static final Logger log = LoggerFactory.getLogger(LocalStackMcpServer.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String endpoint;
    private final String region;

    public LocalStackMcpServer(
            S3StorageService s3StorageService,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.localstack.endpoint}") String endpoint,
            @Value("${aws.s3.region}") String region) {
        // Access the S3Client from S3StorageService via reflection
        try {
            Field s3ClientField = S3StorageService.class.getDeclaredField("s3Client");
            s3ClientField.setAccessible(true);
            this.s3Client = (S3Client) s3ClientField.get(s3StorageService);
        } catch (Exception e) {
            log.error("Failed to access S3Client from S3StorageService: {}", e.getMessage());
            throw new RuntimeException("Cannot initialize LocalStack MCP Server", e);
        }

        this.bucketName = bucketName;
        this.endpoint = endpoint;
        this.region = region;
        log.info("LocalStack MCP Server initialized for bucket: {} at {}", bucketName, endpoint);
    }

    /**
     * Get MCP server information and available tools
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "localstack");
        info.put("version", "1.0.0");
        info.put("description", "MCP server for LocalStack S3 operations");
        info.put("endpoint", endpoint);
        info.put("bucket", bucketName);
        info.put("region", region);

        List<Map<String, Object>> tools = new ArrayList<>();

        // List buckets tool
        tools.add(Map.of(
            "name", "list_buckets",
            "description", "List all S3 buckets in LocalStack",
            "parameters", Map.of()
        ));

        // List objects tool
        tools.add(Map.of(
            "name", "list_objects",
            "description", "List objects in the configured S3 bucket",
            "parameters", Map.of(
                "prefix", Map.of("type", "string", "description", "Filter by prefix (optional)", "required", false),
                "maxKeys", Map.of("type", "integer", "description", "Maximum number of keys to return (default: 100)", "required", false)
            )
        ));

        // Get object metadata tool
        tools.add(Map.of(
            "name", "get_object_metadata",
            "description", "Get metadata for a specific S3 object",
            "parameters", Map.of(
                "key", Map.of("type", "string", "description", "The S3 object key", "required", true)
            )
        ));

        // Check bucket exists tool
        tools.add(Map.of(
            "name", "check_bucket_exists",
            "description", "Check if the configured S3 bucket exists",
            "parameters", Map.of()
        ));

        info.put("tools", tools);

        return ResponseEntity.ok(info);
    }

    /**
     * List all S3 buckets
     */
    @PostMapping("/tools/list_buckets")
    public ResponseEntity<Map<String, Object>> listBuckets() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();

            List<Map<String, Object>> buckets = response.buckets().stream()
                .map(bucket -> Map.<String, Object>of(
                    "name", bucket.name(),
                    "creationDate", bucket.creationDate() != null ? bucket.creationDate().toString() : "unknown"
                ))
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("buckets", buckets);
            result.put("count", buckets.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error listing buckets: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * List objects in the configured S3 bucket
     */
    @PostMapping("/tools/list_objects")
    public ResponseEntity<Map<String, Object>> listObjects(
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            String prefix = params != null && params.containsKey("prefix")
                ? (String) params.get("prefix") : "";
            Integer maxKeys = params != null && params.containsKey("maxKeys")
                ? (Integer) params.get("maxKeys") : 100;

            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(maxKeys);

            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            List<Map<String, Object>> objects = response.contents().stream()
                .map(obj -> Map.<String, Object>of(
                    "key", obj.key(),
                    "size", obj.size(),
                    "lastModified", obj.lastModified().toString(),
                    "storageClass", obj.storageClassAsString(),
                    "etag", obj.eTag()
                ))
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bucket", bucketName);
            result.put("prefix", prefix);
            result.put("objects", objects);
            result.put("count", objects.size());
            result.put("isTruncated", response.isTruncated());

            return ResponseEntity.ok(result);

        } catch (NoSuchBucketException e) {
            log.error("Bucket does not exist: {}", bucketName);
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Bucket does not exist: " + bucketName
            ));
        } catch (Exception e) {
            log.error("Error listing objects: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get metadata for a specific S3 object
     */
    @PostMapping("/tools/get_object_metadata")
    public ResponseEntity<Map<String, Object>> getObjectMetadata(
            @RequestBody Map<String, String> params) {
        try {
            String key = params.get("key");
            if (key == null || key.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Parameter 'key' is required"
                ));
            }

            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            HeadObjectResponse response = s3Client.headObject(request);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("success", true);
            metadata.put("bucket", bucketName);
            metadata.put("key", key);
            metadata.put("contentType", response.contentType());
            metadata.put("contentLength", response.contentLength());
            metadata.put("lastModified", response.lastModified().toString());
            metadata.put("etag", response.eTag());
            metadata.put("storageClass", response.storageClassAsString());
            metadata.put("metadata", response.metadata());

            return ResponseEntity.ok(metadata);

        } catch (NoSuchKeyException e) {
            log.error("Object not found: {}", params.get("key"));
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Object not found: " + params.get("key")
            ));
        } catch (Exception e) {
            log.error("Error getting object metadata: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Check if the configured bucket exists
     */
    @PostMapping("/tools/check_bucket_exists")
    public ResponseEntity<Map<String, Object>> checkBucketExists() {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();

            s3Client.headBucket(request);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "bucket", bucketName,
                "exists", true
            ));

        } catch (NoSuchBucketException e) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "bucket", bucketName,
                "exists", false
            ));
        } catch (Exception e) {
            log.error("Error checking bucket: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            // Try to list buckets to verify connectivity
            s3Client.listBuckets();

            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "localstack-s3",
                "endpoint", endpoint,
                "bucket", bucketName
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "service", "localstack-s3",
                "endpoint", endpoint,
                "error", e.getMessage()
            ));
        }
    }
}

