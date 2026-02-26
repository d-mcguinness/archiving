#!/bin/bash

# Start the Spring Boot application with local profile for MCP servers

echo "=========================================="
echo "Starting Archiving System with MCP Servers"
echo "=========================================="
echo ""

# Check if LocalStack is running
echo "Checking LocalStack..."
if curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "✓ LocalStack is running on port 4566"
else
    echo "⚠️  LocalStack is NOT running on port 4566"
    echo "   Start it with: docker run -d -p 4566:4566 localstack/localstack"
fi

echo ""

# Check if PostgreSQL is running
echo "Checking PostgreSQL..."
if nc -z localhost 5432 2>/dev/null; then
    echo "✓ PostgreSQL is running on port 5432"
else
    echo "⚠️  PostgreSQL is NOT running on port 5432"
    echo "   Start it with:"
    echo "   docker run -d -p 5432:5432 \\"
    echo "     -e POSTGRES_DB=archiving \\"
    echo "     -e POSTGRES_USER=archiving_user \\"
    echo "     -e POSTGRES_PASSWORD=archiving_pass \\"
    echo "     postgres:15"
fi

echo ""
echo "=========================================="
echo "Starting Spring Boot Application"
echo "=========================================="
echo ""
echo "Profile: local"
echo "Port: 2020"
echo "MCP Servers: ENABLED"
echo ""

# Set the profile and start the application
export SPRING_PROFILES_ACTIVE=local

# Start Spring Boot
./mvnw spring-boot:run

