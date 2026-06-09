package com.dmc.archiving.tenancy.api;

import java.util.Set;

/**
 * Single source of truth for which archive standards are "premium" — i.e. whose
 * AIP/DIP generation is a metered, billable capability (currently NOARK5 and
 * E-ARK).
 *
 * <p>Premium-ness is a BILLING policy, not an archival-format property, so it
 * lives in the tenancy (billing) module rather than on the {@code ArchiveStandard}
 * enum. That placement is also load-bearing: the {@code archive} module already
 * depends on {@code tenancy.api}, so a reverse {@code tenancy -> archive} edge
 * would form a module cycle that {@code ModulithStructureTest} rejects.
 *
 * <p>Held as enum NAMES (not {@code ArchiveStandard}) for that same
 * cycle-avoidance reason, and because the persisted {@code aips/dips.standard}
 * column is {@code @Enumerated(EnumType.STRING)} — so these names must equal
 * {@code ArchiveStandard.name()} exactly (asserted by {@code PremiumStandardsTest}).
 * The usage module derives the enum-typed set from these names.
 */
public final class PremiumStandards {

    private PremiumStandards() {}

    /** Archive standards (by enum name) whose AIP/DIP generation is metered/billable. */
    public static final Set<String> NAMES = Set.of("NOARK5", "EARK");

    /**
     * True if the given standard name is a metered premium standard. Fail-safe:
     * an unknown or null-ish name simply returns false (never throws).
     */
    public static boolean contains(String standardName) {
        return NAMES.contains(standardName);
    }
}
