package com.dmc.archiving.dip.generator;

import com.dmc.archiving.dip.model.Dip;

import java.util.Map;

public interface DipGenerator {
    String generate(Dip dip);
    String getStandardName();
    Map<String, Object> buildPackage(Dip dip);
}
