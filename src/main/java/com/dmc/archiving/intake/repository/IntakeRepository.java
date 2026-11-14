package com.dmc.archiving.intake.repository;

import com.dmc.archiving.intake.model.Intake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntakeRepository extends JpaRepository<Intake, Long> {

    List<Intake> findByTenantId(Long tenantId);

    List<Intake> findByOwnerId(Long ownerId);

    // Whether the user owns the intake or is assigned to it
    @Query("SELECT COUNT(i) > 0 FROM Intake i LEFT JOIN i.assignedUsers u "
            + "WHERE i.id = :intakeId AND (i.ownerId = :userId OR u.id = :userId)")
    boolean isOwnerOrAssigned(@Param("intakeId") Long intakeId, @Param("userId") Long userId);
}
