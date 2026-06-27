package com.dmc.archiving.tenancy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "tenants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "domain", nullable = false, unique = true, length = 100)
    private String domain;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private TenantPlan plan;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    // ── Billing linkage (Stripe). Null until the tenant is provisioned in Stripe. ──
    /** Stripe Customer id (cus_...) — one Customer per tenant. */
    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    /** Stripe Subscription id (sub_...) for the tenant's current plan. */
    @Column(name = "stripe_subscription_id", length = 255)
    private String stripeSubscriptionId;

    /**
     * How this tenant is billed: CARD (Checkout, charge automatically) for
     * self-serve, or SEND_INVOICE (net terms, bank/wire) for public-sector buyers
     * who can't pay by card. Null = not yet set (defaults applied at provisioning).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_collection_method", length = 32)
    private BillingCollectionMethod billingCollectionMethod;

    @Embedded
    private TenantSettings settings;

    @Transient
    public String getCreatedAtString() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Transient
    public String getUpdatedAtString() {
        return updatedAt != null ? updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
