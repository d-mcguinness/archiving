package com.dmc.archiving.sip.api;

import java.util.Map;

public record SipExportFile(Long id, String title, Map<String, Object> data) {}
