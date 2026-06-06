package com.dmc.archiving.document;

/**
 * Thrown when a billable upload would push a tenant past their configured
 * overage spend cap and they have not opted in to keep accruing. Distinct from
 * {@link StorageQuotaExceededException} (the plan allotment hard-stop): this is
 * the soft cap on how much billable overage a tenant is willing to incur.
 */
public class SpendCapExceededException extends RuntimeException {
    public SpendCapExceededException(String message) {
        super(message);
    }
}
