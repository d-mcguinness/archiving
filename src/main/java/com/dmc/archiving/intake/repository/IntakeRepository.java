package com.dmc.archiving.intake.repository;

import com.dmc.archiving.intake.model.Intake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntakeRepository extends JpaRepository<Intake, Long> {

    List<Intake> findByTenantId(Long tenantId);

    List<Intake> findByOwnerId(Long ownerId);
}
