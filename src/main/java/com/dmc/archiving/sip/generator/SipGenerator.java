package com.dmc.archiving.sip.generator;

import com.dmc.archiving.sip.model.Sip;

import java.util.Map;

public interface SipGenerator {
    String generate(Sip sip);
    String getStandardName();
    Map<String, Object> buildPackage(Sip sip);
}
