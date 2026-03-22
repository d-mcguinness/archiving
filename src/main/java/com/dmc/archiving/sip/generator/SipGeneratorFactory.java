package com.dmc.archiving.sip.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.sip.generator.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SipGeneratorFactory {

    private final Map<ArchiveStandard, SipGenerator> generators;
    private final DefaultSipGenerator defaultGenerator;

    public SipGeneratorFactory(
            Noark5SipGenerator noark5SipGenerator,
            OaisSipGenerator oaisSipGenerator,
            PremisSipGenerator premisSipGenerator,
            DublinCoreSipGenerator dublinCoreSipGenerator,
            MetsSipGenerator metsSipGenerator,
            EadSipGenerator eadSipGenerator,
            BagitSipGenerator bagitSipGenerator,
            IsadgSipGenerator isadgSipGenerator,
            ModsSipGenerator modsSipGenerator,
            EarkSipGenerator earkSipGenerator,
            DefaultSipGenerator defaultGenerator) {

        this.generators = new HashMap<>();
        this.defaultGenerator = defaultGenerator;

        generators.put(ArchiveStandard.NOARK5, noark5SipGenerator);
        generators.put(ArchiveStandard.OAIS, oaisSipGenerator);
        generators.put(ArchiveStandard.PREMIS, premisSipGenerator);
        generators.put(ArchiveStandard.DUBLIN_CORE, dublinCoreSipGenerator);
        generators.put(ArchiveStandard.METS, metsSipGenerator);
        generators.put(ArchiveStandard.EAD, eadSipGenerator);
        generators.put(ArchiveStandard.BAGIT, bagitSipGenerator);
        generators.put(ArchiveStandard.ISADG, isadgSipGenerator);
        generators.put(ArchiveStandard.MODS, modsSipGenerator);
        generators.put(ArchiveStandard.EARK, earkSipGenerator);
    }

    public SipGenerator getGenerator(ArchiveStandard standard) {
        if (standard == null) {
            return defaultGenerator;
        }
        return generators.getOrDefault(standard, defaultGenerator);
    }
}
