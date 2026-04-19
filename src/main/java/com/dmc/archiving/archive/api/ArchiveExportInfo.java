package com.dmc.archiving.archive.api;

import java.util.Map;

/**
 * Everything the export module needs to write an archive's top-level entry,
 * with the standard-specific payload already rendered.
 */
public record ArchiveExportInfo(
        Long archiveId,
        Long tenantId,
        String title,
        String standard,
        String standardReference,
        Map<String, Object> data
) {}
