package com.dmc.archiving.aip.generator;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractAipGenerator
        extends AbstractPackageGenerator<Aip, Aip>
        implements AipGenerator {

    protected AbstractAipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    protected String packageType() {
        return "aip";
    }

    @Override
    protected Long packageId(Aip aip) {
        return aip.getId();
    }

    @Override
    protected Aip toSnapshot(Aip aip) {
        return aip;
    }
}
