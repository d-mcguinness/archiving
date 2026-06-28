package com.dmc.archiving.intake.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.intake.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class IntakeGeneratorFactory {

    private final Map<ArchiveStandard, IntakeGenerator> generators;
    private final DefaultIntakeGenerator defaultGenerator;

    public IntakeGeneratorFactory(
            Noark5IntakeGenerator noark5IntakeGenerator,
            OaisIntakeGenerator oaisIntakeGenerator,
            PremisIntakeGenerator premisIntakeGenerator,
            DublinCoreIntakeGenerator dublinCoreIntakeGenerator,
            MetsIntakeGenerator metsIntakeGenerator,
            EadIntakeGenerator eadIntakeGenerator,
            BagitIntakeGenerator bagitIntakeGenerator,
            IsadgIntakeGenerator isadgIntakeGenerator,
            ModsIntakeGenerator modsIntakeGenerator,
            EarkIntakeGenerator earkIntakeGenerator,
            DefaultIntakeGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.NOARK5, noark5IntakeGenerator);
        generators.put(ArchiveStandard.OAIS, oaisIntakeGenerator);
        generators.put(ArchiveStandard.PREMIS, premisIntakeGenerator);
        generators.put(ArchiveStandard.DUBLIN_CORE, dublinCoreIntakeGenerator);
        generators.put(ArchiveStandard.METS, metsIntakeGenerator);
        generators.put(ArchiveStandard.EAD, eadIntakeGenerator);
        generators.put(ArchiveStandard.BAGIT, bagitIntakeGenerator);
        generators.put(ArchiveStandard.ISADG, isadgIntakeGenerator);
        generators.put(ArchiveStandard.MODS, modsIntakeGenerator);
        generators.put(ArchiveStandard.EARK, earkIntakeGenerator);
    }

    public IntakeGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
