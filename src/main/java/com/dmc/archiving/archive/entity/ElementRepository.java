package com.dmc.archiving.archive.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElementRepository extends JpaRepository<Element, Long> {

    /**
     * Find all elements for a specific archive
     */
    List<Element> findByArchiveId(Long archiveId);

    /**
     * Find root elements for an archive
     */
    List<Element> findByArchiveIdAndIsRootTrue(Long archiveId);


    /**
     * Find elements by parent
     */
    List<Element> findByParentId(Long parentId);

    /**
     * Find element by archive and identifier
     */
    Optional<Element> findByArchiveIdAndElementIdentifier(Long archiveId, String elementIdentifier);

    /**
     * Find elements by archive and status
     */
    List<Element> findByArchiveIdAndStatus(Long archiveId, String status);

    /**
     * Search elements by title or description within an archive
     */
    @Query("SELECT e FROM Element e WHERE e.archive.id = :archiveId AND " +
           "(LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Element> searchByTitleOrDescription(@Param("archiveId") Long archiveId,
                                           @Param("searchTerm") String searchTerm);

    /**
     * Get hierarchy for an archive (ordered by path)
     */
    @Query("SELECT e FROM Element e WHERE e.archive.id = :archiveId ORDER BY e.sortOrder, e.createdAt")
    List<Element> findHierarchyByArchive(@Param("archiveId") Long archiveId);

    /**
     * Count elements by archive
     */
    long countByArchiveId(Long archiveId);


    /**
     * Find all leaf elements (elements without children)
     */
    @Query("SELECT e FROM Element e WHERE e.archive.id = :archiveId AND " +
           "NOT EXISTS (SELECT c FROM Element c WHERE c.parent.id = e.id)")
    List<Element> findLeafElementsByArchive(@Param("archiveId") Long archiveId);
}


