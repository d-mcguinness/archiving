package com.dmc.archiving.release.api;

import java.util.Map;

public record ReleaseExportFile(Long id, String title, Map<String, Object> data) {}
