package com.dmc.archiving.archive.repository;

import com.dmc.archiving.archive.model.UserAssignment;
import com.dmc.archiving.archive.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAssignmentRepository extends JpaRepository<UserAssignment, Long> {

    // Find all assignments for a specific archive
    List<UserAssignment> findByArchiveId(Long archiveId);

    // Find all assignments for a specific user
    List<UserAssignment> findByUserId(Long userId);

    // Find assignment for a specific user and archive
    Optional<UserAssignment> findByArchiveIdAndUserId(Long archiveId, Long userId);

    // Find all assignments for a specific user with a specific role
    List<UserAssignment> findByUserIdAndRole(Long userId, UserRole role);

    // Find all assignments for a specific archive with a specific role
    List<UserAssignment> findByArchiveIdAndRole(Long archiveId, UserRole role);

    // Check if a user is assigned to an archive
    boolean existsByArchiveIdAndUserId(Long archiveId, Long userId);

    // Check if a user has a specific role in an archive
    boolean existsByArchiveIdAndUserIdAndRole(Long archiveId, Long userId, UserRole role);

    // Count assignments for an archive
    long countByArchiveId(Long archiveId);

    // Count assignments for a user
    long countByUserId(Long userId);

    // Delete all assignments for a specific archive
    void deleteByArchiveId(Long archiveId);

    // Delete all assignments for a specific user
    void deleteByUserId(Long userId);

    // Delete specific assignment
    void deleteByArchiveIdAndUserId(Long archiveId, Long userId);

    // Find archives where user has owner role
    @Query("SELECT ua FROM UserAssignment ua WHERE ua.userId = :userId AND ua.role = 'OWNER'")
    List<UserAssignment> findOwnerAssignmentsByUserId(@Param("userId") Long userId);

    // Find all users with specific role across all archives
    @Query("SELECT DISTINCT ua.userId FROM UserAssignment ua WHERE ua.role = :role")
    List<Long> findUserIdsByRole(@Param("role") UserRole role);

    // Find recent assignments for a user (useful for activity tracking)
    @Query("SELECT ua FROM UserAssignment ua WHERE ua.userId = :userId ORDER BY ua.assignedAt DESC")
    List<UserAssignment> findRecentAssignmentsByUserId(@Param("userId") Long userId);

    // Find assignments ordered by assigned date
    List<UserAssignment> findByArchiveIdOrderByAssignedAtAsc(Long archiveId);

    List<UserAssignment> findByArchiveIdOrderByAssignedAtDesc(Long archiveId);
}
