package com.dmc.archiving.archive;

/**
 * Thrown when creating an archive would push a tenant past its plan's archive
 * allotment and the plan does not permit overage (FREE hard-stop).
 *
 * <p>Extends {@link IllegalStateException} so the GraphQL error handler maps it
 * to a BAD_REQUEST classification with the message intact.
 */
public class ArchiveQuotaExceededException extends IllegalStateException {
    public ArchiveQuotaExceededException(String message) {
        super(message);
    }
}
