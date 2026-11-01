package com.dmc.archiving.preservation.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGeneratorFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PreservationGeneratorFactory extends AbstractPackageGeneratorFactory<PreservationGenerator> {

    public PreservationGeneratorFactory(List<PreservationGenerator> generators) {
        super(generators);
    }
}
