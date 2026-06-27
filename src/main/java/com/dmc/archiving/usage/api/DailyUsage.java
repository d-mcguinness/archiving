package com.dmc.archiving.usage.api;

import java.time.LocalDate;

/**
 * One tenant's billable usage for a single day — the billing-facing projection of
 * a usage snapshot (no internal entity leaks across the module boundary).
 *
 * <p>{@code storageBytes} and {@code seatCount} are STOCK metrics (point-in-time at
 * capture); {@code premiumPackagesGenerated} is a FLOW (count generated that day).
 */
public record DailyUsage(
        LocalDate period,
        long storageBytes,
        long premiumPackagesGenerated,
        long seatCount) {
}
