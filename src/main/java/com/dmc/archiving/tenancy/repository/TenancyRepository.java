package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.Tenant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Acquire a pessimistic write lock on the tenant row (SELECT ... FOR UPDATE),
     * held until the surrounding transaction commits. Used to serialize the
     * read-then-write of per-tenant quota/spend-cap enforcement so concurrent
     * requests for the same tenant cannot both pass a cap.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tenant t WHERE t.id = :id")
    Optional<Tenant> findByIdForUpdate(@Param("id") Long id);
}
