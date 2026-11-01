package com.dmc.archiving.pkg;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.generator.AbstractPackageGeneratorFactory;
import com.dmc.archiving.pkg.generator.PackageGenerator;
import com.dmc.archiving.pkg.model.PackageStage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a generator for a unified {@link com.dmc.archiving.pkg.model.ArchivalPackage}
 * by (stage, standard). It collects every {@link PackageGenerator} bean in the
 * application — the Intake, Preservation and Release modules each contribute
 * theirs — and groups them by {@code packageType()}, so the unified module
 * reuses the stage modules' generators without depending on those modules.
 */
@Component
public class PackageGeneratorRegistry {

    private final Map<PackageStage, StageFactory> byStage = new EnumMap<>(PackageStage.class);

    public PackageGeneratorRegistry(List<PackageGenerator<?>> generators) {
        Map<PackageStage, List<PackageGenerator<?>>> grouped = new EnumMap<>(PackageStage.class);
        for (PackageGenerator<?> g : generators) {
            PackageStage stage = PackageStage.fromPackageType(g.packageType());
            grouped.computeIfAbsent(stage, k -> new ArrayList<>()).add(g);
        }
        grouped.forEach((stage, list) -> byStage.put(stage, new StageFactory(list)));
    }

    public PackageGenerator<?> getGenerator(PackageStage stage, ArchiveStandard standard) {
        StageFactory factory = byStage.get(stage);
        if (factory == null) {
            throw new IllegalStateException("No generators registered for stage " + stage);
        }
        return factory.getGenerator(standard);
    }

    public boolean hasDedicatedGenerator(PackageStage stage, ArchiveStandard standard) {
        StageFactory factory = byStage.get(stage);
        return factory != null && factory.hasDedicatedGenerator(standard);
    }

    private static final class StageFactory extends AbstractPackageGeneratorFactory<PackageGenerator<?>> {
        StageFactory(List<PackageGenerator<?>> generators) {
            super(generators);
        }
    }
}
