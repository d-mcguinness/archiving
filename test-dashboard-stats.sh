#!/bin/bash

# Test Role-Based Dashboard Stats
# Make sure backend is running on localhost:2020

echo "🧪 Testing Role-Based Dashboard Stats"
echo "========================================"
echo ""

# Test 1: Admin Dashboard Stats (All Tenants)
echo "1️⃣  Testing ADMIN Dashboard Stats (System-Wide)"
echo "Query: getDashboardStats"
curl -s -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ getDashboardStats { totalUsers totalTenants totalArchives activeArchives draftArchives archivedArchives } }"
  }' | jq '.'

echo ""
echo "---"
echo ""

# Test 2: Tenant Dashboard Stats (Tenant ID 1)
echo "2️⃣  Testing TENANT Dashboard Stats (Tenant ID: 1)"
echo "Query: getTenantDashboardStats(tenantId: \"1\")"
curl -s -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($id: ID!) { getTenantDashboardStats(tenantId: $id) { tenantId tenantName tenantStatus tenantPlan totalUsers totalArchives activeArchives draftArchives archivedArchives } }",
    "variables": { "id": "1" }
  }' | jq '.'

echo ""
echo "---"
echo ""

# Test 3: Tenant Dashboard Stats (Tenant ID 2)
echo "3️⃣  Testing TENANT Dashboard Stats (Tenant ID: 2)"
echo "Query: getTenantDashboardStats(tenantId: \"2\")"
curl -s -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($id: ID!) { getTenantDashboardStats(tenantId: $id) { tenantId tenantName tenantStatus tenantPlan totalUsers totalArchives activeArchives draftArchives archivedArchives } }",
    "variables": { "id": "2" }
  }' | jq '.'

echo ""
echo "---"
echo ""

# Test 4: Tenant Dashboard Stats (Tenant ID 3)
echo "4️⃣  Testing TENANT Dashboard Stats (Tenant ID: 3)"
echo "Query: getTenantDashboardStats(tenantId: \"3\")"
curl -s -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($id: ID!) { getTenantDashboardStats(tenantId: $id) { tenantId tenantName tenantStatus tenantPlan totalUsers totalArchives activeArchives draftArchives archivedArchives } }",
    "variables": { "id": "3" }
  }' | jq '.'

echo ""
echo "========================================"
echo "✅ Tests Complete!"
echo ""
echo "Expected Results:"
echo "- Admin stats: Combined totals across all tenants"
echo "- Tenant 1 stats: Only Acme Corp's users and archives"
echo "- Tenant 2 stats: Only Tech Innovations' users and archives"
echo "- Tenant 3 stats: Only Global Solutions' users and archives"

