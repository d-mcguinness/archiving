package com.dmc.archiving.intake.generator;

import com.dmc.archiving.pkg.generator.AbstractPackageGeneratorFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IntakeGeneratorFactory extends AbstractPackageGeneratorFactory<IntakeGenerator> {

    public IntakeGeneratorFactory(List<IntakeGenerator> generators) {
        super(generators);
    }
}
