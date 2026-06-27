package com.dmc.archiving.billing.api;

import java.util.List;

/**
 * Public billing contract. Phase 0 establishes the boundary; the implementation
 * (Stripe-backed) arrives in later phases:
 * <ul>
 *   <li>Phase 2 — {@link #createCheckoutSession} / {@link #createPortalSession}
 *       (self-serve activation and management via Stripe Checkout + Customer Portal);</li>
 *   <li>Phase 2 — {@link #invoiceHistory} (invoices reconciled from webhooks).</li>
 * </ul>
 * Methods return plain types (URLs, simple records) so no Stripe type crosses the
 * module boundary.
 */
public interface BillingApi {

    /** Start a Stripe Checkout session to subscribe/upgrade a tenant; returns the hosted URL. */
    String createCheckoutSession(Long tenantId, String targetPlan);

    /** Open the Stripe Customer Portal for a tenant (manage card, cancel, download invoices); returns the URL. */
    String createPortalSession(Long tenantId);

    /** A tenant's invoices, most recent first. */
    List<InvoiceSummary> invoiceHistory(Long tenantId);

    /** Billing-facing invoice projection (no Stripe types leak across the boundary). */
    record InvoiceSummary(String stripeInvoiceId, String status, long amountDueMinor, String currency, String hostedInvoiceUrl) {
    }
}
