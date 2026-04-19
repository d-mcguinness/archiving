package com.dmc.archiving.archive.specification;

import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.model.ArchiveStandard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Search criteria for archives
 * Used with ArchiveSpecifications to build dynamic queries
 */
@Data
@Builder
public class ArchiveSearchCriteria {

    /**
     * Filter by tenant ID
     */
    private Long tenantId;

    /**
     * Filter by owner ID
     */
    private Long ownerId;

    /**
     * Filter by status
     */
    private ArchiveStatus status;

    /**
     * Filter by standard
     */
    private ArchiveStandard standard;

    /**
     * Search keyword (searches in title and description)
     */
    private String keyword;

    /**
     * Filter archives created after this date
     */
    private LocalDateTime fromDate;

    /**
     * Filter archives created before this date
     */
    private LocalDateTime toDate;

    /**
     * Filter archives updated after this date
     */
    private LocalDateTime updatedAfter;

    /**
     * Filter by multiple statuses
     */
    private ArchiveStatus[] statuses;

    /**
     * Filter by multiple standards
     */
    private ArchiveStandard[] standards;
}

