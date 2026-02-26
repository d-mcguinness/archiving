# Document Upload Component Moved to Users Page

## Summary
Moved the file upload component from the dashboard page to the users page, as requested.

## Changes Made

### 1. Dashboard Page (`frontend/src/routes/+page.svelte`)
**Removed:**
- File upload state variables (`selectedFile`, `uploading`, `uploadMessage`, `uploadError`)
- File upload handler functions (`handleFileSelect`, `handleUpload`)
- File upload UI section (the entire file upload card)
- Unused CSS styles for file upload components
- Unused `loadUsers()` function
- Unused user info card styles

**Result:** Dashboard now only displays:
- For ADMIN/TENANT: Stats cards, archive breakdown, and quick actions
- For USER: Welcome message and their document list (view only)
- For guests: Welcome message with login link

### 2. Users Page (`frontend/src/routes/users/+page.svelte`)
**Added:**
- File upload state variables
- File upload handler functions (`handleFileSelect`, `handleUpload`)
- File upload UI section positioned right after the page header
- Complete CSS styles for the upload component
- Auto-reload of users list after successful upload

**Result:** Users page now includes:
- File upload section at the top (📤 Upload Users File)
- User list table below
- Edit and delete functionality for users

## User Experience

### For ADMIN/TENANT Users:
- Navigate to `/users` page to upload user files
- Upload files and see immediate feedback
- User list automatically refreshes after upload
- Can still create individual users via the "Add User" button

### For USER Role:
- Dashboard shows their personal documents (no upload capability)
- Users cannot access the `/users` page (role-based restrictions apply)

## Technical Details

### File Upload Features:
- Visual feedback with icons and messages
- Success/error notifications
- Disabled state during upload
- File selection display
- Automatic form reset after successful upload
- User list reload after upload completes

### API Integration:
- Endpoint: `POST http://localhost:2020/api/upload`
- FormData with file attachment
- Error handling with user-friendly messages

## Testing Recommendations

1. Test file upload on users page as ADMIN
2. Verify user list refreshes after upload
3. Confirm dashboard no longer shows upload component
4. Verify USER role can still view their documents on dashboard
5. Test error scenarios (invalid file, network errors)

