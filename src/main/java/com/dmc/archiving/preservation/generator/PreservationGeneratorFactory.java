package com.dmc.archiving.preservation.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.preservation.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PreservationGeneratorFactory {

    private final Map<ArchiveStandard, PreservationGenerator> generators;
    private final DefaultPreservationGenerator defaultGenerator;

    public PreservationGeneratorFactory(
            EarkPreservationGenerator earkPreservationGenerator,
            Noark5PreservationGenerator noark5PreservationGenerator,
            DefaultPreservationGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.EARK, earkPreservationGenerator);
        generators.put(ArchiveStandard.NOARK5, noark5PreservationGenerator);
    }

    public PreservationGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
