package com.dmc.archiving.document;

/**
 * Thrown when a billable upload would push a tenant past its plan's storage
 * allotment and the plan does not permit overage (FREE hard-stop).
 */
public class StorageQuotaExceededException extends RuntimeException {
    public StorageQuotaExceededException(String message) {
        super(message);
    }
}
