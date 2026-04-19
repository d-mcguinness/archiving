package com.dmc.archiving.tenancy.repository;

import com.dmc.archiving.tenancy.model.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {

    List<TenantMembership> findByTenantId(Long tenantId);

    List<TenantMembership> findByUserId(Long userId);

    boolean existsByTenantIdAndUserId(Long tenantId, Long userId);

    long countByTenantId(Long tenantId);

    void deleteByTenantIdAndUserId(Long tenantId, Long userId);

    void deleteByUserId(Long userId);
}
