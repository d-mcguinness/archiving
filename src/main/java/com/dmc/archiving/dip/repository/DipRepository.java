package com.dmc.archiving.dip.repository;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.dip.model.Dip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DipRepository extends JpaRepository<Dip, Long> {

    List<Dip> findByTenantId(Long tenantId);

    List<Dip> findByOwnerId(Long ownerId);

    // SQL aggregate: count DIPs for a tenant whose standard is in the given set
    // (used to meter premium-standard package generation).
    long countByTenantIdAndStandardIn(Long tenantId, Collection<ArchiveStandard> standards);
}
