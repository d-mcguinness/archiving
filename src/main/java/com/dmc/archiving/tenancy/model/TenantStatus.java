package com.dmc.archiving.tenancy.model;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public enum TenantStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    PENDING_ACTIVATION,
    TRIAL,
    EXPIRED
}
