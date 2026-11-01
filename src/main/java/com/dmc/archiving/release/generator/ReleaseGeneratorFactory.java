package com.dmc.archiving.release.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGeneratorFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReleaseGeneratorFactory extends AbstractPackageGeneratorFactory<ReleaseGenerator> {

    public ReleaseGeneratorFactory(List<ReleaseGenerator> generators) {
        super(generators);
    }
}
