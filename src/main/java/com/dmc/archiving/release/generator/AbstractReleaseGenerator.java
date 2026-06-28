package com.dmc.archiving.release.generator;

import com.dmc.archiving.release.model.Release;
import com.dmc.archiving.pkg.generator.AbstractPackageGenerator;
import com.dmc.archiving.storage.CloudStorageService;

public abstract class AbstractReleaseGenerator
        extends AbstractPackageGenerator<Release, Release>
        implements ReleaseGenerator {

    protected AbstractReleaseGenerator(CloudStorageService cloudStorageService) {
        super(cloudStorageService);
    }

    @Override
    protected String packageType() {
        return "dip";
    }

    @Override
    protected Long packageId(Release dip) {
        return dip.getId();
    }

    @Override
    protected Release toSnapshot(Release dip) {
        return dip;
    }
}
