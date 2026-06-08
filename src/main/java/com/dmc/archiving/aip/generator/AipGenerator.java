package com.dmc.archiving.aip.generator;

import com.dmc.archiving.aip.model.Aip;

public interface AipGenerator {
    String generate(Aip aip);
    String getStandardName();
}
