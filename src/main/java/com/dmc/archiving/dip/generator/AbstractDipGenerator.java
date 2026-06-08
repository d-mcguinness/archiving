package com.dmc.archiving.dip.generator;

import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractDipGenerator
        extends AbstractPackageGenerator<Dip, Dip>
        implements DipGenerator {

    protected AbstractDipGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    protected String packageType() {
        return "dip";
    }

    @Override
    protected Long packageId(Dip dip) {
        return dip.getId();
    }

    @Override
    protected Dip toSnapshot(Dip dip) {
        return dip;
    }
}
