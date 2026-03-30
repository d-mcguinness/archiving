package com.dmc.archiving.pkg.model;

public enum PackageStatus {
    // Common
    DRAFT,
    REJECTED,

    // SIP lifecycle
    SUBMITTED,
    VALIDATED,
    ACCEPTED,

    // AIP lifecycle
    BUILDING,
    STORED,

    // DIP lifecycle
    PREPARED,
    DISSEMINATED,
    EXPIRED
}
