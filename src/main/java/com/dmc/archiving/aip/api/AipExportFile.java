package com.dmc.archiving.aip.api;

import java.util.Map;

public record AipExportFile(Long id, String title, Map<String, Object> data) {}
