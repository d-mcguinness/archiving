package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;

/**
 * A generator turns one archival package into a stored artifact and returns
 * its storage URL. This is the common contract behind the Intake, Preservation
 * and Release generator interfaces, and the unit the unified package module
 * dispatches to.
 *
 * @param <P> the package entity this generator accepts via {@link #generate(Object)}
 */
public interface PackageGenerator<P> {

    /** Generate from a live entity. Equivalent to snapshotting it and calling {@link #generate(PackageSnapshot)}. */
    String generate(P pkg);

    /** Generate from a stage-agnostic snapshot — the entry point for callers that hold no entity of type P. */
    String generate(PackageSnapshot snapshot);

    /** The standard's name (e.g. "EARK", or "DEFAULT" for the fallback). Used in filenames and logs. */
    String getStandardName();

    /**
     * The {@link ArchiveStandard} this generator handles, or null for a
     * fallback generator that serves any standard without a dedicated one.
     */
    ArchiveStandard supportedStandard();

    /** Lifecycle marker — "sip", "aip" or "dip". Drives the storage prefix and filename suffix. */
    String packageType();
}
