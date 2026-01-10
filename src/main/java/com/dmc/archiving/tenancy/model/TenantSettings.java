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
}
