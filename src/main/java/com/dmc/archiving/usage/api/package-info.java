/**
 * Usage Module API - read-only billable usage for the billing layer.
 *
 * Exposes per-tenant daily usage snapshots over a date range so the billing
 * module can compute period totals (GB-month storage, premium-package counts)
 * without reaching into the usage module's internal model/repository.
 */
@org.springframework.modulith.NamedInterface("api")
package com.dmc.archiving.usage.api;
