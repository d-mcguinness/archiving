package com.dmc.archiving.billing.model;

/**
 * Lifecycle of a {@link StripeMeterReport} outbox row.
 *
 * <ul>
 *   <li>{@code PENDING} — computed, not yet pushed to Stripe; eligible for the drain.</li>
 *   <li>{@code SENT} — accepted by Stripe (a meter event was created); never re-pushed.</li>
 *   <li>{@code FAILED} — push errored after retries; needs investigation/reconciliation.</li>
 * </ul>
 */
public enum MeterReportStatus {
    PENDING,
    SENT,
    FAILED
}
