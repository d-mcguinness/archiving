package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenancyRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByDomain(String domain);

    @Query("SELECT t FROM Tenant t WHERE t.ownerId = :ownerId")
    List<Tenant> findByOwnerId(@Param("ownerId") String ownerId);
}
