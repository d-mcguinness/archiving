package com.dmc.archiving.intake.generator;

import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.intake.model.Intake;
import com.dmc.archiving.pkg.generator.PackageGenerator;

import java.util.Map;

public interface IntakeGenerator extends PackageGenerator<Intake> {
    /** Suggested root-element field values for a new Intake, derived from the uploaded file's metadata. */
    Map<String, String> prefillFields(FileMetadataInput meta);
}
