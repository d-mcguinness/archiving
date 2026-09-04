package com.dmc.archiving.preservation.generator;

import com.dmc.archiving.preservation.model.Preservation;
import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractPreservationGenerator
        extends AbstractPackageGenerator<Preservation, Preservation>
        implements PreservationGenerator {

    protected AbstractPreservationGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    protected String packageType() {
        return "aip";
    }

    @Override
    protected Long packageId(Preservation aip) {
        return aip.getId();
    }

    @Override
    protected Preservation toSnapshot(Preservation aip) {
        return aip;
    }
}
