package com.dmc.archiving.common.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s with ID %d not found", resourceType, id));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

