package com.dmc.archiving.tenancy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettings {
    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_archives")
    private Integer maxArchives;

    @Column(name = "max_storage_bytes")
    private Long maxStorageBytes;

    @Column(name = "allow_external_sharing")
    private Boolean allowExternalSharing;

    @Column(name = "enable_audit_log")
    private Boolean enableAuditLog;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "default_language", length = 20)
    private String defaultLanguage;

    @Column(name = "custom_domain", length = 100)
    private String customDomain;

    /**
     * Canonical per-plan default settings — the single source for plan allotments.
     * ENTERPRISE is unlimited (-1). Used both to seed a new tenant's settings and
     * as the fail-closed fallback when a tenant's settings are missing, so the two
     * never drift apart.
     */
    public static TenantSettings defaultsFor(TenantPlan plan) {
        return switch (plan) {
            case FREE -> new TenantSettings(5, 10, 1024L * 1024 * 100, false, false, "UTC", "en", null);
            case BASIC -> new TenantSettings(25, 100, 1024L * 1024 * 1024, true, false, "UTC", "en", null);
            case PROFESSIONAL -> new TenantSettings(100, 1000, 1024L * 1024 * 1024 * 10, true, true, "UTC", "en", null);
            case ENTERPRISE -> new TenantSettings(-1, -1, -1L, true, true, "UTC", "en", null);
            case CUSTOM -> new TenantSettings(50, 500, 1024L * 1024 * 1024 * 5, true, true, "UTC", "en", null);
        };
    }
}
