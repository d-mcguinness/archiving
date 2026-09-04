package com.dmc.archiving.intake.api;

import java.util.Map;

public record IntakeExportFile(Long id, String title, Map<String, Object> data) {}
