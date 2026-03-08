package com.dmc.archiving.tenancy.model;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public enum TenantPlan {
    FREE,
    BASIC,
    PROFESSIONAL,
    ENTERPRISE,
    CUSTOM
}
