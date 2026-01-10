package com.dmc.archiving.archive.model;

public enum UserRole {
    OWNER,        // Can edit, delete, and manage assignments
    EDITOR,       // Can edit content and status
    REVIEWER,     // Can view and comment, change status to reviewed
    VIEWER        // Read-only access
}
