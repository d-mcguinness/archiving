package com.dmc.archiving.preservation.api;

import java.util.Map;

public record PreservationExportFile(Long id, String title, Map<String, Object> data) {}
