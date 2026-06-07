package com.dmc.archiving.document;

/**
 * Thrown when an uploaded file exceeds the per-file size cap for the tenant's
 * plan. Standard plans cap at the default; ENTERPRISE/CUSTOM get the raised
 * large-file ceiling.
 */
public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException(String message) {
        super(message);
    }
}
