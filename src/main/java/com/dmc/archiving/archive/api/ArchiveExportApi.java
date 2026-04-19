package com.dmc.archiving.archive.api;

public interface ArchiveExportApi {
    /** Returns null if the archive does not exist. */
    ArchiveExportInfo loadForExport(Long archiveId);
}
