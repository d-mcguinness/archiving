package com.dmc.archiving.user.repository;

import com.dmc.archiving.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN u.tenants t WHERE t.id = :tenantId")
    List<User> findUsersByTenantId(@Param("tenantId") Long tenantId);
}
