package com.dmc.archiving.aip.repository;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.archive.model.ArchiveStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AipRepository extends JpaRepository<Aip, Long> {

    List<Aip> findByTenantId(Long tenantId);

    List<Aip> findByOwnerId(Long ownerId);

    // SQL aggregate: count AIPs for a tenant whose standard is in the given set
    // (used to meter premium-standard package generation).
    long countByTenantIdAndStandardIn(Long tenantId, Collection<ArchiveStandard> standards);
}
