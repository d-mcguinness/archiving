/**
 * Usage Module - Per-tenant usage metering for billing.
 *
 * Aggregates billable resources (stored bytes, premium package generations,
 * seat count) per tenant using SQL aggregates and persists a nightly snapshot
 * the billing layer reads. Reads other modules through their public services /
 * APIs only; nothing depends on this module.
 */
package com.dmc.archiving.usage;
