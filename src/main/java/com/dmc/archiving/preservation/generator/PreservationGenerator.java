package com.dmc.archiving.preservation.generator;

import com.dmc.archiving.preservation.model.Preservation;

public interface PreservationGenerator {
    String generate(Preservation aip);
    String getStandardName();
}
