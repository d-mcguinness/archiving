-- V2: Stripe billing linkage on tenants (Phase 0 foundation).
-- One Stripe Customer + Subscription per tenant, and the per-tenant collection
-- method (CARD via Checkout, or SEND_INVOICE net-terms for public sector).
-- Nullable: existing tenants are backfilled when first provisioned in Stripe.

ALTER TABLE tenants ADD COLUMN stripe_customer_id VARCHAR(255);
ALTER TABLE tenants ADD COLUMN stripe_subscription_id VARCHAR(255);
ALTER TABLE tenants ADD COLUMN billing_collection_method VARCHAR(32);
