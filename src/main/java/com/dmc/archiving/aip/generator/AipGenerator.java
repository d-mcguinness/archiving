package com.dmc.archiving.aip.generator;

import com.dmc.archiving.aip.model.Aip;

import java.util.Map;

public interface AipGenerator {
    String generate(Aip aip);
    String getStandardName();
    Map<String, Object> buildPackage(Aip aip);
}
