# MCP Servers for LocalStack and DataSource

## Overview

Model Context Protocol (MCP) servers have been added to provide AI assistants with tools to interact with LocalStack S3 and PostgreSQL database when running in the **local** profile.

## What are MCP Servers?

MCP (Model Context Protocol) servers expose REST API endpoints that AI assistants can use to:
- Query database state
- Inspect S3 storage
- Get real-time application context
- Debug and troubleshoot issues

## Implementation

### Components Created

1. **McpServerConfig.java** - Configuration class for MCP servers
2. **LocalStackMcpServer.java** - MCP server for LocalStack S3 operations
3. **DataSourceMcpServer.java** - MCP server for PostgreSQL database operations
4. **McpServerController.java** - Main controller for MCP server discovery and documentation

### Configuration

MCP servers are only active when:
- Spring profile is set to `local`
- For LocalStack MCP: `aws.use-localstack=true`

Configuration in `application-local.properties`:
```properties
# MCP Servers Configuration (for AI assistant integration)
mcp.enabled=true
mcp.localstack.enabled=true
mcp.datasource.enabled=true
```

## Available Endpoints

### Main MCP Endpoints

#### Get All MCP Servers
```bash
GET http://localhost:2020/mcp
```

Returns list of available MCP servers with their endpoints.

#### Get Usage Documentation
```bash
GET http://localhost:2020/mcp/usage
```

Returns comprehensive documentation and examples for all MCP servers.

#### Health Check
```bash
GET http://localhost:2020/mcp/health
```

Check if MCP servers are active.

---

## LocalStack MCP Server

Base URL: `/mcp/localstack`

### Available Tools

#### 1. Get Server Info
```bash
GET http://localhost:2020/mcp/localstack/info
```

Returns server information and list of available tools.

#### 2. List Buckets
```bash
POST http://localhost:2020/mcp/localstack/tools/list_buckets
Content-Type: application/json
```

Lists all S3 buckets in LocalStack.

**Response:**
```json
{
  "success": true,
  "buckets": [
    {
      "name": "archiving-system-uploads",
      "creationDate": "2026-02-26T10:00:00Z"
    }
  ],
  "count": 1
}
```

#### 3. List Objects
```bash
POST http://localhost:2020/mcp/localstack/tools/list_objects
Content-Type: application/json

{
  "prefix": "uploads/users/",
  "maxKeys": 10
}
```

Lists objects in the configured S3 bucket.

**Response:**
```json
{
  "success": true,
  "bucket": "archiving-system-uploads",
  "prefix": "uploads/users/",
  "objects": [
    {
      "key": "uploads/users/1/document.pdf",
      "size": 1024567,
      "lastModified": "2026-02-26T10:30:00Z",
      "storageClass": "STANDARD",
      "etag": "abc123..."
    }
  ],
  "count": 1,
  "isTruncated": false
}
```

#### 4. Get Object Metadata
```bash
POST http://localhost:2020/mcp/localstack/tools/get_object_metadata
Content-Type: application/json

{
  "key": "uploads/users/1/document.pdf"
}
```

Gets metadata for a specific S3 object.

**Response:**
```json
{
  "success": true,
  "bucket": "archiving-system-uploads",
  "key": "uploads/users/1/document.pdf",
  "contentType": "application/pdf",
  "contentLength": 1024567,
  "lastModified": "2026-02-26T10:30:00Z",
  "etag": "abc123...",
  "storageClass": "STANDARD",
  "metadata": {}
}
```

#### 5. Check Bucket Exists
```bash
POST http://localhost:2020/mcp/localstack/tools/check_bucket_exists
Content-Type: application/json
```

Checks if the configured S3 bucket exists.

**Response:**
```json
{
  "success": true,
  "bucket": "archiving-system-uploads",
  "exists": true
}
```

#### 6. Health Check
```bash
GET http://localhost:2020/mcp/localstack/health
```

Checks LocalStack S3 connectivity.

---

## DataSource MCP Server

Base URL: `/mcp/datasource`

### Available Tools

#### 1. Get Server Info
```bash
GET http://localhost:2020/mcp/datasource/info
```

Returns server information and list of available tools.

#### 2. List Tables
```bash
POST http://localhost:2020/mcp/datasource/tools/list_tables
Content-Type: application/json

{
  "schema": "public"
}
```

Lists all tables in the database schema.

**Response:**
```json
{
  "success": true,
  "schema": "public",
  "tables": [
    {
      "schema": "public",
      "name": "users",
      "type": "TABLE",
      "remarks": null
    },
    {
      "schema": "public",
      "name": "tenants",
      "type": "TABLE",
      "remarks": null
    }
  ],
  "count": 2
}
```

#### 3. Describe Table
```bash
POST http://localhost:2020/mcp/datasource/tools/describe_table
Content-Type: application/json

{
  "tableName": "users",
  "schema": "public"
}
```

Gets the schema/structure of a specific table.

**Response:**
```json
{
  "success": true,
  "schema": "public",
  "tableName": "users",
  "columns": [
    {
      "name": "id",
      "type": "BIGINT",
      "size": 19,
      "nullable": false,
      "defaultValue": null,
      "remarks": null
    },
    {
      "name": "username",
      "type": "VARCHAR",
      "size": 50,
      "nullable": false,
      "defaultValue": null,
      "remarks": null
    }
  ],
  "primaryKeys": ["id"],
  "foreignKeys": [
    {
      "column": "tenant_id",
      "referencedTable": "tenants",
      "referencedColumn": "id"
    }
  ]
}
```

#### 4. Execute Query (Read-Only)
```bash
POST http://localhost:2020/mcp/datasource/tools/execute_query
Content-Type: application/json

{
  "query": "SELECT * FROM users LIMIT 5",
  "maxRows": 5
}
```

Executes a SELECT query (read-only for safety).

**Response:**
```json
{
  "success": true,
  "columns": ["id", "username", "email", "tenant_id"],
  "rows": [
    {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "tenant_id": 1
    }
  ],
  "rowCount": 1,
  "maxRows": 5
}
```

**Security Note:** Only SELECT queries are allowed. INSERT, UPDATE, DELETE, etc. will be rejected.

#### 5. Get Database Info
```bash
POST http://localhost:2020/mcp/datasource/tools/get_database_info
Content-Type: application/json
```

Gets database metadata and connection information.

**Response:**
```json
{
  "success": true,
  "databaseProductName": "PostgreSQL",
  "databaseProductVersion": "15.3",
  "driverName": "PostgreSQL JDBC Driver",
  "driverVersion": "42.6.0",
  "jdbcUrl": "jdbc:postgresql://localhost:5432/archiving",
  "username": "archiving_user",
  "maxConnections": 0,
  "supportsTransactions": true
}
```

#### 6. Get Table Count
```bash
POST http://localhost:2020/mcp/datasource/tools/get_table_count
Content-Type: application/json

{
  "tableName": "users",
  "schema": "public"
}
```

Gets the row count for a specific table.

**Response:**
```json
{
  "success": true,
  "schema": "public",
  "tableName": "users",
  "count": 25
}
```

#### 7. Health Check
```bash
GET http://localhost:2020/mcp/datasource/health
```

Checks database connectivity.

---

## Usage Examples

### Using curl

#### List all S3 objects
```bash
curl -X POST http://localhost:2020/mcp/localstack/tools/list_objects \
  -H "Content-Type: application/json" \
  -d '{"prefix": "uploads/", "maxKeys": 10}'
```

#### Query database
```bash
curl -X POST http://localhost:2020/mcp/datasource/tools/execute_query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT COUNT(*) as total FROM users", "maxRows": 1}'
```

#### List all tables
```bash
curl -X POST http://localhost:2020/mcp/datasource/tools/list_tables \
  -H "Content-Type: application/json" \
  -d '{"schema": "public"}'
```

### Using in AI Assistants

AI assistants can use these endpoints to:

1. **Debugging**: Check if files are stored correctly in S3
   ```
   POST /mcp/localstack/tools/list_objects
   {"prefix": "uploads/users/1/"}
   ```

2. **Data Inspection**: Verify database state
   ```
   POST /mcp/datasource/tools/execute_query
   {"query": "SELECT * FROM users WHERE id = 1"}
   ```

3. **Schema Discovery**: Understand database structure
   ```
   POST /mcp/datasource/tools/describe_table
   {"tableName": "users"}
   ```

---

## Security Considerations

### Profile-Based Activation
- MCP servers ONLY run in `local` profile
- Never active in production, staging, or other environments
- Controlled via `@ConditionalOnProperty` annotations

### Read-Only Database Access
- DataSource MCP server only allows SELECT queries
- INSERT, UPDATE, DELETE queries are rejected
- Table names are sanitized to prevent SQL injection

### No Authentication (Local Only)
- Since MCP servers only run locally, no authentication is implemented
- Do NOT enable in production environments
- Always run behind firewall in local development

### CORS
- MCP endpoints follow same CORS configuration as main application
- Only accessible from allowed origins (localhost)

---

## Troubleshooting

### MCP Servers Not Available

**Check 1: Profile**
```bash
# Verify you're running with local profile
echo $SPRING_PROFILES_ACTIVE
# Should output: local
```

**Check 2: LocalStack**
```bash
# Verify LocalStack is running
curl http://localhost:4566/_localstack/health
```

**Check 3: Database**
```bash
# Verify PostgreSQL is running
psql -h localhost -U archiving_user -d archiving -c "SELECT 1"
```

**Check 4: Application Logs**
```bash
# Look for MCP server initialization messages
./mvnw spring-boot:run | grep MCP
```

Expected output:
```
LocalStack MCP Server initialized for bucket: archiving-system-uploads at http://localhost:4566
DataSource MCP Server initialized for database: jdbc:postgresql://localhost:5432/archiving
```

### Health Check Fails

```bash
# Check LocalStack health
curl http://localhost:2020/mcp/localstack/health

# Check DataSource health
curl http://localhost:2020/mcp/datasource/health
```

---

## Implementation Details

### Technology Stack
- **Spring Boot 3.5.4**
- **Spring Web** for REST endpoints
- **PostgreSQL JDBC** for database access
- **AWS SDK for Java 2.x** for S3 operations
- **Reflection** to access S3Client from S3StorageService

### Code Structure
```
src/main/java/com/dmc/archiving/mcp/
├── McpServerConfig.java           # Configuration
├── McpServerController.java       # Main discovery endpoint
├── LocalStackMcpServer.java       # LocalStack S3 tools
└── DataSourceMcpServer.java       # PostgreSQL tools
```

### Conditional Activation
```java
// LocalStack MCP Server
@ConditionalOnProperty(name = "aws.use-localstack", havingValue = "true")

// DataSource MCP Server
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "local")
```

---

## Future Enhancements

Potential improvements for MCP servers:

1. **Authentication**: Add API key or JWT authentication
2. **Rate Limiting**: Prevent abuse of endpoints
3. **Audit Logging**: Log all MCP server operations
4. **Caching**: Cache frequently requested data
5. **WebSocket Support**: Real-time updates
6. **GraphQL Support**: Query data with GraphQL
7. **OpenAPI Documentation**: Auto-generated API docs
8. **Metrics**: Track MCP server usage
9. **Write Operations**: Allow controlled write operations with approval
10. **Multi-Profile Support**: Enable in dev and staging with authentication

---

## Testing

### Start Application with Local Profile
```bash
cd /Users/dmcg/workspace2/archiving

# Set profile
export SPRING_PROFILES_ACTIVE=local

# Start LocalStack
docker run -d -p 4566:4566 localstack/localstack

# Start PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=archiving \
  -e POSTGRES_USER=archiving_user \
  -e POSTGRES_PASSWORD=archiving_pass \
  postgres:15

# Start Spring Boot
./mvnw spring-boot:run
```

### Test MCP Servers
```bash
# 1. Check MCP servers are active
curl http://localhost:2020/mcp

# 2. Test LocalStack MCP
curl http://localhost:2020/mcp/localstack/info
curl -X POST http://localhost:2020/mcp/localstack/tools/list_buckets

# 3. Test DataSource MCP
curl http://localhost:2020/mcp/datasource/info
curl -X POST http://localhost:2020/mcp/datasource/tools/list_tables \
  -H "Content-Type: application/json" \
  -d '{"schema": "public"}'

# 4. Health checks
curl http://localhost:2020/mcp/localstack/health
curl http://localhost:2020/mcp/datasource/health
```

---

## Summary

✅ **MCP Servers Created**: LocalStack and DataSource  
✅ **Profile-Based Activation**: Only in local profile  
✅ **Security**: Read-only database access, no production exposure  
✅ **Documentation**: Complete API documentation  
✅ **Tools Available**: 11+ tools for AI assistant integration  

MCP servers provide a powerful way for AI assistants to understand and interact with your application's state during local development!

---

**Date**: February 26, 2026  
**Status**: ✅ **COMPLETE**

