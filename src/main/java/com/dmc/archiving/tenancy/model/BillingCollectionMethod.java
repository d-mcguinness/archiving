package com.dmc.archiving.tenancy.model;

/**
 * How a tenant's Stripe subscription collects payment.
 *
 * <ul>
 *   <li>{@code CARD} — charge automatically against a card captured via Stripe
 *       Checkout (self-serve signups).</li>
 *   <li>{@code SEND_INVOICE} — Stripe sends an invoice with net terms (bank/wire),
 *       for public-sector / enterprise buyers who cannot pay by card.</li>
 * </ul>
 */
public enum BillingCollectionMethod {
    CARD,
    SEND_INVOICE
}
