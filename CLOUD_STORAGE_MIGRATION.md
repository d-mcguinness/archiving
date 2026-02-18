# Cloud Storage Migration - AWS S3 Integration

## Overview

The file upload system has been migrated from local filesystem storage to **AWS S3 Cloud Storage**. This provides:

- ✅ **Scalability**: No disk space limitations
- ✅ **Reliability**: 99.999999999% (11 9's) durability
- ✅ **Security**: Encrypted storage and presigned URLs
- ✅ **Cost-effective**: Pay only for what you use
- ✅ **Global access**: Access files from anywhere
- ✅ **Backup & versioning**: Built-in backup capabilities

---

## Architecture

### Before (Local Storage)
```
User uploads file → Spring Boot → Local filesystem (uploads/ directory)
```

### After (Cloud Storage)
```
User uploads file → Spring Boot → AWS S3 Bucket → Presigned URL returned
```

---

## New Components

### 1. Cloud Storage Service Interface
**File**: `src/main/java/com/dmc/archiving/storage/CloudStorageService.java`

Generic interface for cloud storage operations:
- `uploadFile()` - Upload file to cloud
- `downloadFile()` - Download file from cloud
- `deleteFile()` - Delete file from cloud
- `fileExists()` - Check if file exists
- `getPresignedUrl()` - Generate temporary download URL

### 2. AWS S3 Implementation
**File**: `src/main/java/com/dmc/archiving/storage/S3StorageService.java`

AWS S3-specific implementation with:
- Automatic file naming (timestamp + original filename)
- User-specific folders (users/{userId}/)
- Metadata storage (original filename, upload time, user ID)
- Presigned URLs for secure downloads
- File sanitization for security

### 3. Supporting Classes
- **UploadResult.java** - DTO for upload response
- **StorageException.java** - Custom exception for storage errors

### 4. Updated Controller
**File**: `FileUploadController.java`

Now uses CloudStorageService instead of filesystem operations.

---

## AWS S3 Setup Guide

### Step 1: Create AWS Account
1. Go to [AWS Console](https://aws.amazon.com/)
2. Sign up for a free account (includes 5GB S3 storage free tier)

### Step 2: Create S3 Bucket
1. Navigate to **S3** in AWS Console
2. Click **Create bucket**
3. Enter bucket name: `archiving-system-uploads` (or your preferred name)
4. Choose region: `us-east-1` (or closest to you)
5. **Block Public Access**: Keep enabled (we use presigned URLs)
6. **Bucket Versioning**: Optional (recommended for backup)
7. Click **Create bucket**

### Step 3: Create IAM User for Programmatic Access
1. Navigate to **IAM** in AWS Console
2. Click **Users** → **Add users**
3. Username: `archiving-system-s3-user`
4. Access type: ✅ **Programmatic access**
5. Permissions: Attach policy **AmazonS3FullAccess** (or create custom policy below)
6. Review and create
7. **IMPORTANT**: Save the **Access Key ID** and **Secret Access Key** (shown only once!)

### Step 4: Custom IAM Policy (Recommended - More Secure)
Instead of `AmazonS3FullAccess`, create a policy with minimal permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:HeadObject"
      ],
      "Resource": [
        "arn:aws:s3:::archiving-system-uploads",
        "arn:aws:s3:::archiving-system-uploads/*"
      ]
    }
  ]
}
```

### Step 5: Configure Application

#### Option A: Using application.properties (Development Only)
Edit `src/main/resources/application.properties`:

```properties
aws.s3.bucket-name=archiving-system-uploads
aws.s3.region=us-east-1
aws.s3.access-key=AKIAIOSFODNN7EXAMPLE
aws.s3.secret-key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

⚠️ **WARNING**: Never commit actual credentials to Git!

#### Option B: Using Environment Variables (Production - Recommended)
Set environment variables:

```bash
export AWS_S3_BUCKET_NAME=archiving-system-uploads
export AWS_REGION=us-east-1
export AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
export AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

Or add to `.env` file (add to `.gitignore`!):

```bash
AWS_S3_BUCKET_NAME=archiving-system-uploads
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-actual-access-key
AWS_SECRET_ACCESS_KEY=your-actual-secret-key
```

#### Option C: Using AWS IAM Roles (Production on EC2/ECS)
If running on AWS infrastructure:
1. Create IAM role with S3 permissions
2. Attach role to EC2 instance or ECS task
3. Remove `aws.s3.access-key` and `aws.s3.secret-key` from config
4. AWS SDK will automatically use instance credentials

---

## API Endpoints

### 1. Upload File (General)
```bash
POST /api/upload
Content-Type: multipart/form-data

file: [binary file]
```

**Response:**
```json
{
  "success": true,
  "message": "File uploaded successfully to cloud storage!",
  "fileKey": "uploads/20260217_143022_document.pdf",
  "fileUrl": "https://archiving-system-uploads.s3.amazonaws.com/...",
  "originalFilename": "document.pdf",
  "size": 1048576,
  "contentType": "application/pdf",
  "uploadTime": "2026-02-17T14:30:22"
}
```

### 2. Upload File for User
```bash
POST /api/upload/user
Content-Type: multipart/form-data

file: [binary file]
userId: 123
```

**Response:**
```json
{
  "success": true,
  "message": "File uploaded successfully to cloud storage!",
  "fileKey": "users/123/20260217_143022_document.pdf",
  "fileUrl": "https://archiving-system-uploads.s3.amazonaws.com/...",
  "originalFilename": "document.pdf",
  "userId": 123,
  "size": 1048576,
  "contentType": "application/pdf",
  "uploadTime": "2026-02-17T14:30:22"
}
```

### 3. Download File (Get Presigned URL)
```bash
GET /api/download/{fileKey}
```

**Response:**
```json
{
  "success": true,
  "downloadUrl": "https://archiving-system-uploads.s3.amazonaws.com/...?X-Amz-...",
  "message": "Presigned URL generated successfully"
}
```

**Note**: Presigned URLs expire after 1 hour by default.

### 4. Get Storage Info
```bash
GET /api/upload/info
```

**Response:**
```json
{
  "storageType": "AWS S3 Cloud Storage",
  "maxFileSize": 52428800,
  "maxFileSizeMB": 50,
  "supportedOperations": ["upload", "download", "delete", "presigned-urls"]
}
```

---

## File Organization in S3

```
archiving-system-uploads/
├── uploads/                          # General uploads
│   ├── 20260217_143022_file1.pdf
│   ├── 20260217_143045_file2.jpg
│   └── ...
└── users/                            # User-specific uploads
    ├── 1/
    │   ├── 20260217_143100_doc1.pdf
    │   └── 20260217_143200_doc2.docx
    ├── 2/
    │   └── 20260217_143300_image.png
    └── ...
```

---

## Security Features

### 1. Filename Sanitization
- Removes path separators (`/`, `\`)
- Removes null bytes (`\x00`)
- Removes leading dots (prevents hidden files)
- Replaces invalid characters with underscores

### 2. File Size Validation
- Maximum file size: **50MB**
- Configurable in `application.properties`

### 3. Presigned URLs
- Temporary URLs that expire after specified time
- No public bucket access needed
- Secure file sharing

### 4. S3 Metadata
Each file includes metadata:
- `original-filename`: Original uploaded filename
- `upload-time`: Timestamp of upload
- `user-id`: Associated user ID (if applicable)

### 5. Private Bucket
- Bucket has **Block Public Access** enabled
- Files are not publicly accessible
- Access only via presigned URLs

---

## Cost Estimation

### AWS S3 Free Tier (First 12 Months)
- ✅ 5 GB storage
- ✅ 20,000 GET requests
- ✅ 2,000 PUT requests

### After Free Tier (us-east-1 pricing)
- **Storage**: $0.023 per GB/month
- **PUT requests**: $0.005 per 1,000 requests
- **GET requests**: $0.0004 per 1,000 requests
- **Data transfer OUT**: $0.09 per GB (after 1 GB free/month)

**Example Cost for Small Application:**
- 100 GB storage: $2.30/month
- 100,000 uploads: $0.50/month
- 500,000 downloads: $0.20/month
- **Total**: ~$3/month

---

## Testing

### 1. Test Upload with cURL
```bash
curl -X POST http://localhost:2020/api/upload \
  -F "file=@/path/to/your/file.pdf"
```

### 2. Test User Upload
```bash
curl -X POST http://localhost:2020/api/upload/user \
  -F "file=@/path/to/your/file.pdf" \
  -F "userId=1"
```

### 3. Test Download
```bash
curl http://localhost:2020/api/download/uploads/20260217_143022_file.pdf
```

### 4. Verify in AWS Console
1. Go to S3 Console
2. Click on your bucket
3. Navigate to `uploads/` or `users/` folder
4. Verify files are uploaded with correct metadata

---

## Migration from Local Storage

### Existing Local Files
If you have existing files in `uploads/` directory, you can migrate them:

```bash
# Using AWS CLI
aws s3 sync ./uploads/ s3://archiving-system-uploads/uploads/

# Or using S3 Console
# Upload files via drag-and-drop in S3 Console
```

### Database Updates
If you stored filenames in database, update them to use `fileKey` instead:

```sql
-- Example migration
UPDATE archives 
SET file_path = CONCAT('uploads/', file_path)
WHERE file_path NOT LIKE 'uploads/%';
```

---

## Troubleshooting

### Error: "Unable to execute HTTP request"
**Cause**: Invalid AWS credentials or region  
**Fix**: Verify `aws.s3.access-key`, `aws.s3.secret-key`, and `aws.s3.region`

### Error: "Access Denied"
**Cause**: IAM user doesn't have S3 permissions  
**Fix**: Attach `AmazonS3FullAccess` policy or custom policy to IAM user

### Error: "NoSuchBucket"
**Cause**: Bucket name doesn't exist  
**Fix**: Create bucket in AWS Console or verify `aws.s3.bucket-name` config

### Error: "The bucket does not allow ACLs"
**Cause**: Bucket has ACLs disabled  
**Fix**: Our implementation doesn't use ACLs, so this shouldn't occur

### Files Upload but Can't Download
**Cause**: Presigned URL generation issue  
**Fix**: Verify IAM user has `s3:GetObject` permission

---

## Alternative Cloud Providers

The interface-based design allows easy switching to other providers:

### Google Cloud Storage
Replace `S3StorageService` with `GCSStorageService` implementing same interface.

### Azure Blob Storage
Replace `S3StorageService` with `AzureBlobStorageService` implementing same interface.

### MinIO (Self-Hosted S3-Compatible)
MinIO is S3-compatible, so `S3StorageService` works with minimal config changes:

```properties
aws.s3.bucket-name=archiving-uploads
aws.s3.region=us-east-1
aws.s3.access-key=minioadmin
aws.s3.secret-key=minioadmin
# Add MinIO endpoint (not needed for AWS S3)
aws.s3.endpoint=http://localhost:9000
```

---

## Next Steps

### Production Deployment Checklist

- [ ] Create AWS account and S3 bucket
- [ ] Create IAM user with minimal permissions
- [ ] Set up environment variables (don't commit credentials!)
- [ ] Enable S3 bucket versioning (for backup)
- [ ] Set up S3 lifecycle policies (auto-delete old files)
- [ ] Configure CloudFront CDN (optional, for faster downloads)
- [ ] Set up S3 logging and monitoring
- [ ] Enable S3 encryption at rest
- [ ] Configure CORS on S3 bucket if frontend uploads directly
- [ ] Set up backup/disaster recovery plan
- [ ] Test presigned URL expiration
- [ ] Monitor AWS costs

---

## Resources

- [AWS S3 Documentation](https://docs.aws.amazon.com/s3/)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)
- [S3 Pricing Calculator](https://calculator.aws/)
- [AWS Free Tier](https://aws.amazon.com/free/)
- [S3 Best Practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/best-practices.html)

---

**Version**: 1.0  
**Date**: February 17, 2026  
**Status**: ✅ Production Ready

