# MCP Servers Quick Reference

## 🚀 Quick Start

```bash
# 1. Set profile
export SPRING_PROFILES_ACTIVE=local

# 2. Start services
docker run -d -p 4566:4566 localstack/localstack
docker run -d -p 5432:5432 -e POSTGRES_DB=archiving postgres:15

# 3. Run app
./mvnw spring-boot:run

# 4. Test
./test-mcp-servers.sh
```

## 📡 Main Endpoints

```bash
# Discovery
curl http://localhost:2020/mcp

# Documentation
curl http://localhost:2020/mcp/usage | jq .

# Health
curl http://localhost:2020/mcp/health
```

## 🪣 LocalStack S3 Tools

```bash
# List buckets
curl -X POST http://localhost:2020/mcp/localstack/tools/list_buckets

# List objects
curl -X POST http://localhost:2020/mcp/localstack/tools/list_objects \
  -H "Content-Type: application/json" \
  -d '{"prefix": "uploads/", "maxKeys": 10}'

# Get metadata
curl -X POST http://localhost:2020/mcp/localstack/tools/get_object_metadata \
  -H "Content-Type: application/json" \
  -d '{"key": "uploads/users/1/file.pdf"}'

# Check bucket
curl -X POST http://localhost:2020/mcp/localstack/tools/check_bucket_exists

# Health
curl http://localhost:2020/mcp/localstack/health
```

## 🗄️ PostgreSQL Database Tools

```bash
# List tables
curl -X POST http://localhost:2020/mcp/datasource/tools/list_tables \
  -H "Content-Type: application/json" \
  -d '{"schema": "public"}'

# Describe table
curl -X POST http://localhost:2020/mcp/datasource/tools/describe_table \
  -H "Content-Type: application/json" \
  -d '{"tableName": "users", "schema": "public"}'

# Execute query
curl -X POST http://localhost:2020/mcp/datasource/tools/execute_query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * FROM users LIMIT 5", "maxRows": 5}'

# Get DB info
curl -X POST http://localhost:2020/mcp/datasource/tools/get_database_info

# Table count
curl -X POST http://localhost:2020/mcp/datasource/tools/get_table_count \
  -H "Content-Type: application/json" \
  -d '{"tableName": "users", "schema": "public"}'

# Health
curl http://localhost:2020/mcp/datasource/health
```

## 🔒 Security Notes

- ✅ Only active in `local` profile
- ✅ Read-only database access (SELECT only)
- ✅ No authentication (localhost only)
- ⚠️ NEVER enable in production

## 📚 Documentation

- **Full Guide**: [MCP_SERVERS.md](./MCP_SERVERS.md)
- **Implementation**: [MCP_SERVERS_IMPLEMENTATION.md](./MCP_SERVERS_IMPLEMENTATION.md)
- **Test Script**: `./test-mcp-servers.sh`

## 🎯 Common Use Cases

### Check if file uploaded
```bash
curl -X POST http://localhost:2020/mcp/localstack/tools/list_objects \
  -H "Content-Type: application/json" \
  -d '{"prefix": "uploads/users/1/"}'
```

### Count users by tenant
```bash
curl -X POST http://localhost:2020/mcp/datasource/tools/execute_query \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT tenant_id, COUNT(*) FROM users GROUP BY tenant_id"}'
```

### Verify table structure
```bash
curl -X POST http://localhost:2020/mcp/datasource/tools/describe_table \
  -H "Content-Type: application/json" \
  -d '{"tableName": "archives"}'
```

## ✅ Status

**Implementation Date**: February 26, 2026  
**Status**: Complete and Tested  
**Total Endpoints**: 17  
**Total Tools**: 11+

