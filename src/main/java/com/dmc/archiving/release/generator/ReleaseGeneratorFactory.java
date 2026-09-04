package com.dmc.archiving.release.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.release.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ReleaseGeneratorFactory {

    private final Map<ArchiveStandard, ReleaseGenerator> generators;
    private final DefaultReleaseGenerator defaultGenerator;

    public ReleaseGeneratorFactory(
            EarkReleaseGenerator earkReleaseGenerator,
            Noark5ReleaseGenerator noark5ReleaseGenerator,
            DefaultReleaseGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.EARK, earkReleaseGenerator);
        generators.put(ArchiveStandard.NOARK5, noark5ReleaseGenerator);
    }

    public ReleaseGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
