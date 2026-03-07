package com.dmc.archiving.sip.repository;

import com.dmc.archiving.sip.model.Sip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SipRepository extends JpaRepository<Sip, Long> {

    List<Sip> findByTenantId(Long tenantId);

    List<Sip> findByOwnerId(Long ownerId);
}
