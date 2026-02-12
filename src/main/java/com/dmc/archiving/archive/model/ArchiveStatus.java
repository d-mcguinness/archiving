package com.dmc.archiving.archive.model;

import org.springframework.modulith.NamedInterface;

@NamedInterface
public enum ArchiveStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED,
    DELETED
}
