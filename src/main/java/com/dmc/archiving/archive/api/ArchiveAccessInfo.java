package com.dmc.archiving.archive.api;

/**
 * What another module needs to decide whether a user may put content into an
 * archive: the tenant that owns it, and whether the user owns or is assigned to it.
 */
public record ArchiveAccessInfo(Long archiveId, Long tenantId, boolean ownerOrAssigned) {
}
