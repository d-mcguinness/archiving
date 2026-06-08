package com.dmc.archiving.dip.generator;

import com.dmc.archiving.dip.model.Dip;

public interface DipGenerator {
    String generate(Dip dip);
    String getStandardName();
}
