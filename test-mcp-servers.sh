#!/bin/bash

# Test script for MCP Servers
# This script tests all MCP server endpoints when running in local profile

BASE_URL="http://localhost:2020"
PASSED=0
FAILED=0

echo "=========================================="
echo "MCP Servers Test Suite"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4

    echo -n "Testing: $description... "

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi

    # Extract HTTP code (last line) and body (everything except last line)
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $http_code)"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $http_code)"
        echo "Response: $body"
        ((FAILED++))
    fi
}

echo "=========================================="
echo "Main MCP Endpoints"
echo "=========================================="
echo ""

test_endpoint "GET" "/mcp" "" "List all MCP servers"
test_endpoint "GET" "/mcp/health" "" "MCP health check"
test_endpoint "GET" "/mcp/usage" "" "Get MCP usage documentation"

echo ""
echo "=========================================="
echo "LocalStack MCP Server"
echo "=========================================="
echo ""

test_endpoint "GET" "/mcp/localstack/info" "" "Get LocalStack server info"
test_endpoint "GET" "/mcp/localstack/health" "" "LocalStack health check"
test_endpoint "POST" "/mcp/localstack/tools/list_buckets" "{}" "List S3 buckets"
test_endpoint "POST" "/mcp/localstack/tools/list_objects" '{"prefix":"","maxKeys":10}' "List S3 objects"
test_endpoint "POST" "/mcp/localstack/tools/check_bucket_exists" "{}" "Check bucket exists"

echo ""
echo "=========================================="
echo "DataSource MCP Server"
echo "=========================================="
echo ""

test_endpoint "GET" "/mcp/datasource/info" "" "Get DataSource server info"
test_endpoint "GET" "/mcp/datasource/health" "" "DataSource health check"
test_endpoint "POST" "/mcp/datasource/tools/list_tables" '{"schema":"public"}' "List database tables"
test_endpoint "POST" "/mcp/datasource/tools/get_database_info" "{}" "Get database info"

# Test with a simple query
test_endpoint "POST" "/mcp/datasource/tools/execute_query" '{"query":"SELECT 1 as test","maxRows":1}' "Execute SELECT query"

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi

