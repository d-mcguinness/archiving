package com.dmc.archiving.aip.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.aip.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AipGeneratorFactory {

    private final Map<ArchiveStandard, AipGenerator> generators;
    private final DefaultAipGenerator defaultGenerator;

    public AipGeneratorFactory(
            EarkAipGenerator earkAipGenerator,
            DefaultAipGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.EARK, earkAipGenerator);
    }

    public AipGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
