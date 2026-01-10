package com.dmc.archiving.archive.repository;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArchiveRepository extends JpaRepository<Archive, Long> {

    // Find archives by owner
    List<Archive> findByOwnerId(Long ownerId);

    // Find archives by status
    List<Archive> findByStatus(ArchiveStatus status);

    // Find archives where user is assigned (through UserAssignment)
    @Query("SELECT DISTINCT a FROM Archive a JOIN a.assignedUsers ua WHERE ua.userId = :userId")
    List<Archive> findArchivesByUserAssignment(@Param("userId") Long userId);

    // Find archives by owner or assigned user
    @Query("SELECT DISTINCT a FROM Archive a LEFT JOIN a.assignedUsers ua WHERE a.ownerId = :userId OR ua.userId = :userId")
    List<Archive> findArchivesByUserIdOwnerOrAssigned(@Param("userId") Long userId);

    // Find archives by user and role
    @Query("SELECT DISTINCT a FROM Archive a JOIN a.assignedUsers ua WHERE ua.userId = :userId AND ua.role = :role")
    List<Archive> findArchivesByUserIdAndRole(@Param("userId") Long userId, @Param("role") com.dmc.archiving.archive.model.UserRole role);

    // Find archives by title containing (case insensitive)
    List<Archive> findByTitleContainingIgnoreCase(String title);

    // Find archives by owner and status
    List<Archive> findByOwnerIdAndStatus(Long ownerId, ArchiveStatus status);
}
