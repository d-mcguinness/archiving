package com.dmc.archiving.usage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row per tenant per period (capture date). Holds the billable usage
 * metrics derived from SQL aggregates so the billing layer never has to count
 * live entities.
 */
@Entity
@Table(
    name = "usage_snapshots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "period"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    /** The day this snapshot covers; one snapshot per tenant per day. */
    @Column(name = "period", nullable = false)
    private LocalDate period;

    @Column(name = "storage_bytes", nullable = false)
    private long storageBytes;

    /** Count of premium-standard (NOARK5/E-ARK) AIP + DIP packages. */
    @Column(name = "premium_package_count", nullable = false)
    private long premiumPackageCount;

    @Column(name = "seat_count", nullable = false)
    private long seatCount;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;
}
