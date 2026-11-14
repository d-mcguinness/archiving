package com.dmc.archiving.archive.repository;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Archive entities
 * Extends JpaSpecificationExecutor for dynamic query support
 */
@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long>, JpaSpecificationExecutor<Archive> {

    // ========== Paginated Queries (Primary methods for scalability) ==========

    // Override default findAll with pagination support
    @EntityGraph(attributePaths = {"assignedUsers"})
    Page<Archive> findAll(Pageable pageable);

    // Find archives by owner with pagination
    @EntityGraph(attributePaths = {"assignedUsers"})
    Page<Archive> findByOwnerId(Long ownerId, Pageable pageable);

    // Find archives by status with pagination
    @EntityGraph(attributePaths = {"assignedUsers"})
    Page<Archive> findByStatus(ArchiveStatus status, Pageable pageable);

    // Find archives by owner and status with pagination
    @EntityGraph(attributePaths = {"assignedUsers"})
    Page<Archive> findByOwnerIdAndStatus(Long ownerId, ArchiveStatus status, Pageable pageable);

    // Find archives by title containing with pagination
    @EntityGraph(attributePaths = {"assignedUsers"})
    Page<Archive> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Find archives where user is assigned with pagination
    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN FETCH a.assignedUsers u WHERE u.id = :userId")
    Page<Archive> findArchivesByUserAssignment(@Param("userId") Long userId, Pageable pageable);

    // Find archives by owner or assigned user with pagination
    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN FETCH a.assignedUsers u WHERE a.ownerId = :userId OR u.id = :userId")
    Page<Archive> findArchivesByUserIdOwnerOrAssigned(@Param("userId") Long userId, Pageable pageable);

    // ========== Non-Paginated Queries (Backward compatibility) ==========

    // Find archives by owner (legacy - prefer paginated version)
    List<Archive> findByOwnerId(Long ownerId);

    // Find archives by status (legacy - prefer paginated version)
    List<Archive> findByStatus(ArchiveStatus status);

    // Find archives where user is assigned
    @Query("SELECT DISTINCT a FROM Archive a JOIN a.assignedUsers u WHERE u.id = :userId")
    List<Archive> findArchivesByUserAssignment(@Param("userId") Long userId);

    // Find archives by owner or assigned user
    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN a.assignedUsers u WHERE a.ownerId = :userId OR u.id = :userId")
    List<Archive> findArchivesByUserIdOwnerOrAssigned(@Param("userId") Long userId);

    // Whether the user owns the archive or is assigned to it
    @Query("SELECT COUNT(a) > 0 FROM Archive a LEFT JOIN a.assignedUsers u "
            + "WHERE a.id = :archiveId AND (a.ownerId = :userId OR u.id = :userId)")
    boolean isOwnerOrAssigned(@Param("archiveId") Long archiveId, @Param("userId") Long userId);

    // Find archives by title containing (case insensitive)
    List<Archive> findByTitleContainingIgnoreCase(String title);

    // Find archives by owner and status
    List<Archive> findByOwnerIdAndStatus(Long ownerId, ArchiveStatus status);

    // Find archives by tenant
    List<Archive> findByTenantId(Long tenantId);

    // Find archives with a root element (SIPs)
    List<Archive> findByRootElementIsNotNull();

    // Find SIPs by tenant
    @Query("SELECT a FROM Archive a WHERE a.tenantId = :tenantId AND a.rootElement IS NOT NULL")
    List<Archive> findIntakesByTenantId(@Param("tenantId") Long tenantId);

    // ========== Reads whose results are CACHED by ArchiveService ==========
    // Cached entities outlive their persistence context, so every relation a
    // GraphQL selection can reach from Archive must already be loaded when the
    // entity goes into the cache. Otherwise a later request that selects it hits
    // LazyInitializationException while graphql-java iterates the collection —
    // outside the data fetcher, so it surfaces as an HTTP 500, not a GraphQL error.

    @EntityGraph(attributePaths = {"assignedUsers"})
    @Query("SELECT a FROM Archive a WHERE a.id = :id")
    Optional<Archive> findByIdForCache(@Param("id") Long id);

    @EntityGraph(attributePaths = {"assignedUsers"})
    @Query("SELECT DISTINCT a FROM Archive a WHERE a.tenantId = :tenantId")
    List<Archive> findByTenantIdForCache(@Param("tenantId") Long tenantId);

    // ========== Performance Optimization Queries ==========

    // Fetch archives with all relationships for detailed view (prevents N+1)
    @EntityGraph(attributePaths = {"assignedUsers", "elements"})
    @Query("SELECT a FROM Archive a WHERE a.id = :id")
    Archive findByIdWithRelations(@Param("id") Long id);

    // Count queries for pagination metadata
    long countByOwnerId(Long ownerId);
    long countByStatus(ArchiveStatus status);
    long countByOwnerIdAndStatus(Long ownerId, ArchiveStatus status);

    // Count archives in a tenant (for plan quota enforcement)
    long countByTenantId(Long tenantId);
}
