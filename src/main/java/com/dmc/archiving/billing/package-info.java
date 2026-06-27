/**
 * Billing Module - Stripe-backed metered billing.
 *
 * Owns the integration with Stripe: one Customer + Subscription per tenant, and
 * reporting the real meters (storage GB-month, premium NOARK5/E-ARK package count)
 * to Stripe Billing Meters. Reads usage and tenancy only through their public APIs
 * ({@code usage.api}, {@code tenancy.api}); NOTHING depends on billing, so there is
 * no cycle.
 *
 * <p>Phase 0 (this commit) establishes the module, the Stripe SDK + fail-closed
 * secret config, and the tenant billing-linkage columns. Metering, lifecycle, and
 * webhooks land in later phases.
 */
package com.dmc.archiving.billing;
