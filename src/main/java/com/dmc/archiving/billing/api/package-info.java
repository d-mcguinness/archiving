/**
 * Billing Module API - public surface for billing operations.
 *
 * The contract a future web layer (and other modules, should any ever need it)
 * uses to drive billing: start a Checkout session, open the Customer Portal, read
 * invoices. Implemented across later phases; the boundary is established now so
 * the module's public surface is stable.
 */
@org.springframework.modulith.NamedInterface("api")
package com.dmc.archiving.billing.api;
