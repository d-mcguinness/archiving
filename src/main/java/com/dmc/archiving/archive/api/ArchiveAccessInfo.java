package com.dmc.archiving.archive.api;

/**
 * What another module needs to decide whether a user may put content into an
 * archive: the tenant that owns it, whether the user owns or is assigned to it,
 * and the standard its content is described in (the {@code ArchiveStandard} name).
 */
public record ArchiveAccessInfo(Long archiveId, Long tenantId, boolean ownerOrAssigned, String standard) {

    public ArchiveAccessInfo(Long archiveId, Long tenantId, boolean ownerOrAssigned) {
        this(archiveId, tenantId, ownerOrAssigned, null);
    }
}
