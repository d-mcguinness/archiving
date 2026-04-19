package com.dmc.archiving.dip.api;

import java.util.Map;

public record DipExportFile(Long id, String title, Map<String, Object> data) {}
