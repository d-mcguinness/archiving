#!/bin/bash
# Seed LocalStack S3 with placeholder files for all documents in data.sql
# Run this after starting LocalStack to make downloads work in dev

ENDPOINT="http://localhost:4566"
BUCKET="archiving-system-uploads"
REGION="us-east-1"

AWS="aws --endpoint-url=$ENDPOINT --region=$REGION"

echo "=== Seeding LocalStack S3 with sample documents ==="

# Create bucket if it doesn't exist
$AWS s3 mb s3://$BUCKET 2>/dev/null || true

# Helper: create a placeholder file with content matching the type
create_file() {
  local key="$1"
  local content_type="$2"
  local title="$3"
  local size="$4"

  # Create a temp file with sample content
  local tmpfile=$(mktemp)

  case "$content_type" in
    application/pdf)
      echo "%PDF-1.4 Sample PDF - $title" > "$tmpfile"
      ;;
    application/json)
      echo "{\"title\": \"$title\", \"type\": \"sample\", \"generated\": true}" > "$tmpfile"
      ;;
    text/plain)
      echo "Sample text document: $title" > "$tmpfile"
      ;;
    text/csv)
      echo "id,name,description" > "$tmpfile"
      echo "1,Sample,$title" >> "$tmpfile"
      ;;
    image/png|image/jpeg)
      # Create a tiny valid image-like placeholder
      echo "PLACEHOLDER IMAGE - $title" > "$tmpfile"
      ;;
    video/mp4)
      echo "PLACEHOLDER VIDEO - $title" > "$tmpfile"
      ;;
    application/zip)
      echo "PLACEHOLDER ZIP - $title" > "$tmpfile"
      ;;
    *)
      echo "Sample file: $title" > "$tmpfile"
      ;;
  esac

  $AWS s3 cp "$tmpfile" "s3://$BUCKET/$key" \
    --content-type "$content_type" \
    --quiet 2>/dev/null

  if [ $? -eq 0 ]; then
    echo "  ✓ $key"
  else
    echo "  ✗ FAILED: $key"
  fi

  rm -f "$tmpfile"
}

echo ""
echo "Uploading 20 sample documents..."
echo ""

# Document 1 - Q1 Financial Summary
create_file "tenants/1/archives/1/q1-financial-summary.pdf" "application/pdf" "Q1 Financial Summary" 2048576

# Document 2 - Annual Report Draft
create_file "tenants/1/users/2/annual-report-draft.docx" "application/vnd.openxmlformats-officedocument.wordprocessingml.document" "Annual Report Draft" 1536000

# Document 3 - Research Data Analysis
create_file "tenants/2/archives/3/research-analysis.xlsx" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" "Research Data Analysis" 3145728

# Document 4 - Project Presentation
create_file "tenants/3/users/4/project-presentation.pptx" "application/vnd.openxmlformats-officedocument.presentationml.presentation" "Project Presentation" 5242880

# Document 5 - Meeting Notes
create_file "tenants/4/users/5/meeting-notes.txt" "text/plain" "Meeting Notes" 51200

# Document 6 - Budget Breakdown
create_file "tenants/1/archives/2/budget-2026-breakdown.xlsx" "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" "Budget Breakdown Spreadsheet" 2621440

# Document 7 - Old Marketing Plan
create_file "tenants/1/users/2/marketing-plan-2025.pdf" "application/pdf" "Old Marketing Plan" 1843200

# Document 8 - Research White Paper
create_file "tenants/2/archives/4/ml-applications-whitepaper.pdf" "application/pdf" "Research White Paper" 4194304

# Document 9 - Company Logo
create_file "tenants/2/archives/3/tech-innovations-logo.png" "image/png" "Company Logo High Res" 524288

# Document 10 - Scanned Historical Contract
create_file "tenants/3/archives/5/contract-1995-scanned.pdf" "application/pdf" "Scanned Historical Contract" 15728640

# Document 11 - CEO Town Hall Recording
create_file "tenants/3/archives/6/ceo-townhall-q1-2026.mp4" "video/mp4" "CEO Town Hall Recording" 157286400

# Document 12 - Product Specifications
create_file "tenants/4/archives/7/product-specs-v2.0.pdf" "application/pdf" "Product Specifications v2.0" 3670016

# Document 13 - Employee Handbook
create_file "tenants/4/archives/8/employee-handbook-2026.pdf" "application/pdf" "Employee Handbook 2026" 2097152

# Document 14 - Library Catalog
create_file "tenants/4/archives/9/library-catalog-export.csv" "text/csv" "Library Catalog Export" 1048576

# Document 15 - Project Template Draft
create_file "tenants/1/archives/10/project-template-draft.docx" "application/vnd.openxmlformats-officedocument.wordprocessingml.document" "Project Template Draft" 819200

# Document 16 - Campaign Assets Archive
create_file "tenants/2/archives/11/campaign-2025-assets.zip" "application/zip" "Campaign Assets Archive" 52428800

# Document 17 - Onboarding Training Module 1
create_file "tenants/3/archives/12/onboarding-module-1.mp4" "video/mp4" "Onboarding Training Module 1" 209715200

# Document 18 - Office Floor Plan
create_file "tenants/1/users/1/office-floor-plan-2026.jpg" "image/jpeg" "Office Floor Plan" 2621440

# Document 19 - API Configuration
create_file "tenants/2/users/3/api-config.json" "application/json" "API Configuration" 16384

# Document 20 - Q2 Strategy Deck
create_file "tenants/3/users/4/q2-strategy-deck.pptx" "application/vnd.openxmlformats-officedocument.presentationml.presentation" "Q2 Strategy Deck" 7340032

echo ""
echo "=== Done! Listing bucket contents ==="
$AWS s3 ls s3://$BUCKET/ --recursive --human-readable
