package com.dmc.archiving.archive.api;

/**
 * Read-only archive access facts for other modules (e.g. linking a document to
 * an archive). The caller applies its own role rules on top.
 */
public interface ArchiveAccessApi {

    /** Returns null if the archive does not exist. */
    ArchiveAccessInfo accessFor(Long archiveId, Long userId);
}
