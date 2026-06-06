package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.TenantOverageBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantOverageBudgetRepository extends JpaRepository<TenantOverageBudget, Long> {
    Optional<TenantOverageBudget> findByTenantId(Long tenantId);
}
