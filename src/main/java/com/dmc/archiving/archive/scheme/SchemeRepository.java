package com.dmc.archiving.archive.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {

    /**
     * Find all schemes for a specific standard
     */
    List<Scheme> findByStandard(ArchiveStandard standard);

    /**
     * Find root scheme for a specific standard
     */
    Optional<Scheme> findByStandardAndIsRootTrue(ArchiveStandard standard);

    /**
     * Find scheme by standard and name
     */
    Optional<Scheme> findByStandardAndEntityName(ArchiveStandard standard, String entityName);

    /**
     * Find all children of a specific scheme
     */
    List<Scheme> findByParentId(Long parentId);

    /**
     * Find all root-level schemes across all standards
     */
    List<Scheme> findByIsRootTrue();

    /**
     * Get full hierarchy for a standard
     */
    @Query("SELECT e FROM Scheme e WHERE e.standard = :standard ORDER BY e.sortOrder, e.entityName")
    List<Scheme> findHierarchyByStandard(@Param("standard") ArchiveStandard standard);

    /**
     * Check if scheme exists in standard
     */
    boolean existsByStandardAndEntityName(ArchiveStandard standard, String entityName);
}

