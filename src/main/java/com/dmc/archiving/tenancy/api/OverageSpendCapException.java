package com.dmc.archiving.tenancy.api;

/**
 * Thrown when creating a premium package would push a tenant past their
 * premium-package spend cap and they have not opted in to keep accruing.
 *
 * <p>Extends {@link IllegalStateException} so the GraphQL error handler maps it
 * to a BAD_REQUEST classification with the message intact.
 */
public class OverageSpendCapException extends IllegalStateException {
    public OverageSpendCapException(String message) {
        super(message);
    }
}
