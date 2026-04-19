package com.dmc.archiving.archive.specification;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.model.ArchiveStandard;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Specifications for building dynamic Archive queries
 * Uses JPA Criteria API for type-safe, composable queries
 */
public class ArchiveSpecifications {

    /**
     * Filter by tenant ID
     */
    public static Specification<Archive> hasTenantId(Long tenantId) {
        return (root, query, criteriaBuilder) ->
                tenantId == null ? null : criteriaBuilder.equal(root.get("tenantId"), tenantId);
    }

    /**
     * Filter by owner ID
     */
    public static Specification<Archive> hasOwnerId(Long ownerId) {
        return (root, query, criteriaBuilder) ->
                ownerId == null ? null : criteriaBuilder.equal(root.get("ownerId"), ownerId);
    }

    /**
     * Filter by status
     */
    public static Specification<Archive> hasStatus(ArchiveStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Filter by standard
     */
    public static Specification<Archive> hasStandard(ArchiveStandard standard) {
        return (root, query, criteriaBuilder) ->
                standard == null ? null : criteriaBuilder.equal(root.get("standard"), standard);
    }

    /**
     * Search by title (case-insensitive, partial match)
     */
    public static Specification<Archive> titleContains(String keyword) {
        return (root, query, criteriaBuilder) ->
                keyword == null || keyword.isEmpty() ? null :
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("title")),
                                "%" + keyword.toLowerCase() + "%"
                        );
    }

    /**
     * Search by description (case-insensitive, partial match)
     */
    public static Specification<Archive> descriptionContains(String keyword) {
        return (root, query, criteriaBuilder) ->
                keyword == null || keyword.isEmpty() ? null :
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")),
                                "%" + keyword.toLowerCase() + "%"
                        );
    }

    /**
     * Filter archives created after a specific date
     */
    public static Specification<Archive> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) ->
                date == null ? null :
                        criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    /**
     * Filter archives created before a specific date
     */
    public static Specification<Archive> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) ->
                date == null ? null :
                        criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    /**
     * Filter archives updated after a specific date
     */
    public static Specification<Archive> updatedAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) ->
                date == null ? null :
                        criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), date);
    }

    /**
     * Combine title and description search
     */
    public static Specification<Archive> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) ->
                keyword == null || keyword.isEmpty() ? null :
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("title")),
                                        "%" + keyword.toLowerCase() + "%"
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("description")),
                                        "%" + keyword.toLowerCase() + "%"
                                )
                        );
    }

    /**
     * Filter by multiple statuses
     */
    public static Specification<Archive> hasStatusIn(ArchiveStatus... statuses) {
        return (root, query, criteriaBuilder) ->
                statuses == null || statuses.length == 0 ? null :
                        root.get("status").in((Object[]) statuses);
    }

    /**
     * Filter by multiple standards
     */
    public static Specification<Archive> hasStandardIn(ArchiveStandard... standards) {
        return (root, query, criteriaBuilder) ->
                standards == null || standards.length == 0 ? null :
                        root.get("standard").in((Object[]) standards);
    }
}

