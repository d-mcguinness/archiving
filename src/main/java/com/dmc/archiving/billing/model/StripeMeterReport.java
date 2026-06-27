package com.dmc.archiving.billing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outbox row: one billable meter quantity for a tenant + billing period, awaiting
 * push to Stripe. Computed transactionally from usage snapshots, then drained
 * asynchronously to Stripe Billing Meters so a Stripe outage never blocks the app
 * and a retry never double-bills.
 *
 * <p>Idempotency: {@code idempotencyKey} = {@code "{tenantId}:{periodStart}:{meterName}"}
 * is unique — recomputing a period upserts the same row rather than creating a
 * duplicate, and a row already {@code SENT} is never overwritten or re-pushed.
 *
 * <p>{@code quantity} units depend on the meter: for {@code storage_gb_month} it is
 * milli-GB-months (GB-months × 1000, integer — so the Stripe per-unit price is the
 * GB-month rate ÷ 1000); for {@code premium_packages} it is a plain package count.
 */
@Entity
@Table(name = "stripe_meter_reports", indexes = {
        @Index(name = "idx_meter_report_status", columnList = "status"),
        @Index(name = "uq_meter_report_idem", columnList = "idempotency_key", unique = true)
})
@Data
@NoArgsConstructor
public class StripeMeterReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "meter_name", nullable = false, length = 64)
    private String meterName;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Integer meter quantity (milli-GB-months for storage; package count for premium). */
    @Column(name = "quantity", nullable = false)
    private long quantity;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MeterReportStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    /** The Stripe meter-event identifier once pushed; null while PENDING. */
    @Column(name = "stripe_event_id", length = 255)
    private String stripeEventId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
