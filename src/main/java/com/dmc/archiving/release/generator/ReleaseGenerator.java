package com.dmc.archiving.release.generator;

import com.dmc.archiving.release.model.Release;

public interface ReleaseGenerator {
    String generate(Release dip);
    String getStandardName();
}
