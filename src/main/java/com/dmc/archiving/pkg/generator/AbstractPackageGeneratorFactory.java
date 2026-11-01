package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Standard → generator lookup with a single fallback, built from whatever
 * generator beans Spring hands the subclass. Each lifecycle module declares a
 * one-line subclass typed on its own generator interface; registering a new
 * standard is then just adding a {@code @Component} — no factory edit.
 *
 * @param <G> the lifecycle-specific generator interface (IntakeGenerator, …)
 */
public abstract class AbstractPackageGeneratorFactory<G extends PackageGenerator<?>> {

    private final Map<ArchiveStandard, G> generators = new EnumMap<>(ArchiveStandard.class);
    private final G defaultGenerator;

    protected AbstractPackageGeneratorFactory(Collection<? extends G> candidates) {
        G fallback = null;
        for (G g : candidates) {
            ArchiveStandard std = g.supportedStandard();
            if (std == null) {
                if (fallback != null) {
                    throw new IllegalStateException("Two fallback generators registered: "
                            + fallback.getClass().getSimpleName() + " and " + g.getClass().getSimpleName());
                }
                fallback = g;
            } else {
                G previous = generators.putIfAbsent(std, g);
                if (previous != null) {
                    throw new IllegalStateException("Two generators registered for " + std + ": "
                            + previous.getClass().getSimpleName() + " and " + g.getClass().getSimpleName());
                }
            }
        }
        if (fallback == null) {
            throw new IllegalStateException(getClass().getSimpleName()
                    + " needs exactly one fallback generator (supportedStandard() == null)");
        }
        this.defaultGenerator = fallback;
    }

    /** The generator for {@code standard}, or the fallback when none is dedicated (or standard is null). */
    public G getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }

    /** True when a dedicated (non-fallback) generator exists for {@code standard}. */
    public boolean hasDedicatedGenerator(ArchiveStandard standard) {
        return standard != null && generators.containsKey(standard);
    }
}
