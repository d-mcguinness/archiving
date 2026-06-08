package com.dmc.archiving.tenancy.api;

/**
 * The kind of premium package a metering event records. Lives in tenancy.api
 * (the billing module) rather than referencing the aip/dip modules, keeping the
 * ledger free of a dependency on them.
 */
public enum PremiumPackageType {
    AIP,
    DIP
}
