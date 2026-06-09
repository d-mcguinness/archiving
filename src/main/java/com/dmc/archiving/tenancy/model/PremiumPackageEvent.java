package com.dmc.archiving.tenancy.model;

import com.dmc.archiving.tenancy.api.PremiumPackageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Append-only ledger of billable premium-package GENERATION events — one
 * immutable row per generated, billable NOARK5/E-ARK AIP or DIP.
 *
 * <p>The billing meter and the overage cap read this ledger instead of counting
 * live {@code aips}/{@code dips} rows, so deleting a package can never lower a
 * billed counter (the period's recorded generations are fixed) nor free a slot
 * to evade the cap (a generation is a billable event that happened). Rows are
 * never updated or deleted. Only billable premium generations are recorded, so
 * a plain {@code countByTenantId} is already the premium-only total — no
 * standard/billable filter is needed at read time.
 */
@Entity
@Table(name = "premium_package_events", indexes = {
        @Index(name = "idx_ppe_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ppe_tenant_generated", columnList = "tenant_id, generated_at")
})
@Getter
@NoArgsConstructor
public class PremiumPackageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 8)
    private PremiumPackageType packageType;

    /** Archive standard name (enum-string), e.g. NOARK5 / EARK. */
    @Column(name = "standard", nullable = false, length = 32)
    private String standard;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    public PremiumPackageEvent(Long tenantId, PremiumPackageType packageType, String standard,
                               LocalDateTime generatedAt) {
        this.tenantId = tenantId;
        this.packageType = packageType;
        this.standard = standard;
        this.generatedAt = generatedAt;
    }
}
