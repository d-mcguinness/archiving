package com.dmc.archiving.mcp;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Main MCP Server Controller
 * Provides discovery and information about available MCP servers
 * Only active in local profile
 */
@RestController
@RequestMapping("/mcp")
@Profile("local")
public class McpServerController {

    /**
     * Get list of available MCP servers
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listServers() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Archiving System MCP Servers");
        response.put("version", "1.0.0");
        response.put("description", "MCP servers for AI assistant integration with LocalStack and PostgreSQL");

        List<Map<String, Object>> servers = new ArrayList<>();

        // LocalStack MCP Server
        servers.add(Map.of(
            "name", "localstack",
            "description", "MCP server for LocalStack S3 operations",
            "baseUrl", "/mcp/localstack",
            "infoUrl", "/mcp/localstack/info",
            "healthUrl", "/mcp/localstack/health",
            "enabled", true
        ));

        // DataSource MCP Server
        servers.add(Map.of(
            "name", "datasource",
            "description", "MCP server for PostgreSQL database operations",
            "baseUrl", "/mcp/datasource",
            "infoUrl", "/mcp/datasource/info",
            "healthUrl", "/mcp/datasource/health",
            "enabled", true
        ));

        response.put("servers", servers);
        response.put("serverCount", servers.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check for all MCP servers
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "mcp-servers");
        response.put("profile", "local");
        response.put("description", "MCP servers are active in local profile");

        return ResponseEntity.ok(response);
    }

    /**
     * Get MCP server usage information
     */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsage() {
        Map<String, Object> usage = new HashMap<>();
        usage.put("description", "Model Context Protocol (MCP) Servers for AI Integration");

        // LocalStack usage
        Map<String, Object> localStackUsage = new HashMap<>();
        localStackUsage.put("description", "Interact with LocalStack S3 for file storage operations");
        localStackUsage.put("endpoints", List.of(
            "GET /mcp/localstack/info - Get server information and available tools",
            "POST /mcp/localstack/tools/list_buckets - List all S3 buckets",
            "POST /mcp/localstack/tools/list_objects - List objects in bucket",
            "POST /mcp/localstack/tools/get_object_metadata - Get object metadata",
            "POST /mcp/localstack/tools/check_bucket_exists - Check if bucket exists",
            "GET /mcp/localstack/health - Health check"
        ));
        localStackUsage.put("example", Map.of(
            "listObjects", "curl -X POST http://localhost:2020/mcp/localstack/tools/list_objects -H 'Content-Type: application/json' -d '{\"prefix\": \"uploads/\", \"maxKeys\": 10}'",
            "getMetadata", "curl -X POST http://localhost:2020/mcp/localstack/tools/get_object_metadata -H 'Content-Type: application/json' -d '{\"key\": \"uploads/users/1/file.pdf\"}'"
        ));

        // DataSource usage
        Map<String, Object> datasourceUsage = new HashMap<>();
        datasourceUsage.put("description", "Query and inspect PostgreSQL database");
        datasourceUsage.put("endpoints", List.of(
            "GET /mcp/datasource/info - Get server information and available tools",
            "POST /mcp/datasource/tools/list_tables - List all tables",
            "POST /mcp/datasource/tools/describe_table - Get table schema",
            "POST /mcp/datasource/tools/execute_query - Execute SELECT query (read-only)",
            "POST /mcp/datasource/tools/get_database_info - Get database metadata",
            "POST /mcp/datasource/tools/get_table_count - Get row count for a table",
            "GET /mcp/datasource/health - Health check"
        ));
        datasourceUsage.put("example", Map.of(
            "listTables", "curl -X POST http://localhost:2020/mcp/datasource/tools/list_tables -H 'Content-Type: application/json' -d '{\"schema\": \"public\"}'",
            "executeQuery", "curl -X POST http://localhost:2020/mcp/datasource/tools/execute_query -H 'Content-Type: application/json' -d '{\"query\": \"SELECT * FROM users LIMIT 5\", \"maxRows\": 5}'"
        ));

        usage.put("localstack", localStackUsage);
        usage.put("datasource", datasourceUsage);

        usage.put("notes", List.of(
            "MCP servers are only active when running with 'local' profile",
            "All datasource queries are read-only (SELECT only) for safety",
            "Use these endpoints to provide context to AI assistants about your application state"
        ));

        return ResponseEntity.ok(usage);
    }
}

