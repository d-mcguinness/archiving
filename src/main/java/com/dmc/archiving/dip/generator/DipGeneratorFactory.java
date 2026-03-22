package com.dmc.archiving.dip.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.dip.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DipGeneratorFactory {

    private final Map<ArchiveStandard, DipGenerator> generators;
    private final DefaultDipGenerator defaultGenerator;

    public DipGeneratorFactory(
            EarkDipGenerator earkDipGenerator,
            DefaultDipGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.EARK, earkDipGenerator);
    }

    public DipGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
