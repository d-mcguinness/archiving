#!/bin/bash

# Archive System Startup Script
# This script starts the database and Spring Boot backend

echo "🚀 Starting Archive System..."

# Navigate to project directory
cd "$(dirname "$0")"

# Step 1: Start PostgreSQL Database
echo "📦 Step 1: Starting PostgreSQL database..."
docker compose up -d db

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
sleep 5

# Check if database is running
if docker compose ps db | grep -q "Up"; then
    echo "✅ Database is running"
else
    echo "❌ Database failed to start"
    echo "Run: docker compose logs db"
    exit 1
fi

# Step 2: Start Spring Boot Application
echo "🍃 Step 2: Starting Spring Boot application..."
mvn spring-boot:run

# Note: This will keep running in foreground
# Press Ctrl+C to stop the application
