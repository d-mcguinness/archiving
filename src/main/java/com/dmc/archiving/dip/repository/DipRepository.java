package com.dmc.archiving.dip.repository;

import com.dmc.archiving.dip.model.Dip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DipRepository extends JpaRepository<Dip, Long> {

    List<Dip> findByTenantId(Long tenantId);

    List<Dip> findByOwnerId(Long ownerId);
}
