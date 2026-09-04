package com.dmc.archiving.intake.generator;

import com.dmc.archiving.intake.input.FileMetadataInput;
import com.dmc.archiving.intake.model.Intake;

import java.util.Map;

public interface IntakeGenerator {
    String generate(Intake sip);
    String getStandardName();
    Map<String, String> prefillFields(FileMetadataInput meta);
}
