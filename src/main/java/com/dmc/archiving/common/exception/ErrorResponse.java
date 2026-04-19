package com.dmc.archiving.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response format for all API errors
 */
@Data
@Builder
public class ErrorResponse {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error type/category
     */
    private String error;

    /**
     * Human-readable error message
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;

    /**
     * Field-specific validation errors (optional)
     * Key: field name, Value: error message
     */
    private Map<String, String> validationErrors;

    /**
     * Additional error details (optional)
     */
    private Map<String, Object> details;
}

