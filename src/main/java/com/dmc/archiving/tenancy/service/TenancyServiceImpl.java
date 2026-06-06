package com.dmc.archiving.tenancy.service;

import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.input.UpdateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantMembership;
import com.dmc.archiving.tenancy.model.TenantPlan;
import com.dmc.archiving.tenancy.model.TenantSettings;
import com.dmc.archiving.tenancy.model.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dmc.archiving.tenancy.repository.TenancyRepository;
import com.dmc.archiving.tenancy.repository.TenantMembershipRepository;
import com.dmc.archiving.user.api.UserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenancyServiceImpl implements TenancyService {

    private static final Logger log = LoggerFactory.getLogger(TenancyServiceImpl.class);

    private final TenancyRepository tenancyRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserApi userApi;

    public TenancyServiceImpl(TenancyRepository tenancyRepository,
                              TenantMembershipRepository membershipRepository,
                              UserApi userApi) {
        this.tenancyRepository = tenancyRepository;
        this.membershipRepository = membershipRepository;
        this.userApi = userApi;
    }

    @Override
    public Tenant createTenant(CreateTenantInput input) {
        Long ownerIdLong = Long.valueOf(input.getOwnerId());
        if (!userApi.userExists(ownerIdLong)) {
            throw new IllegalArgumentException("Owner user with ID " + input.getOwnerId() + " does not exist");
        }

        if (tenancyRepository.findByDomain(input.getDomain()).isPresent()) {
            throw new IllegalArgumentException("Tenant with domain " + input.getDomain() + " already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        TenantSettings defaultSettings = createDefaultSettings(input.getPlan());

        Tenant tenant = new Tenant(
            null,
            input.getName(),
            input.getDomain(),
            input.getDisplayName(),
            input.getDescription(),
            TenantStatus.ACTIVE,
            input.getPlan(),
            now,
            now,
            String.valueOf(input.getOwnerId()),
            defaultSettings
        );

        return tenancyRepository.save(tenant);
    }

    @Override
    public Tenant updateTenant(UpdateTenantInput input) {
        Tenant tenant = tenancyRepository.findById(input.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant with ID " + input.getTenantId() + " does not exist"));

        if (input.getName() != null) {
            tenant.setName(input.getName());
        }
        if (input.getDisplayName() != null) {
            tenant.setDisplayName(input.getDisplayName());
        }
        if (input.getDescription() != null) {
            tenant.setDescription(input.getDescription());
        }
        if (input.getStatus() != null) {
            tenant.setStatus(input.getStatus());
        }
        if (input.getPlan() != null) {
            tenant.setPlan(input.getPlan());
            tenant.setSettings(createDefaultSettings(input.getPlan()));
        }

        tenant.setUpdatedAt(LocalDateTime.now());
        return tenancyRepository.save(tenant);
    }

    @Override
    public boolean isTenantActive(Long tenantId) {
        Tenant tenant = tenancyRepository.findById(tenantId).orElse(null);
        return tenant != null && tenant.getStatus() == TenantStatus.ACTIVE;
    }

    @Override
    public Tenant getTenantById(Long tenantId) {
        return tenancyRepository.findById(tenantId).orElse(null);
    }

    @Override
    public boolean isUserInTenant(Long userId, Long tenantId) {
        return membershipRepository.existsByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    public boolean deleteTenant(Long id) {
        if (tenancyRepository.existsById(id)) {
            tenancyRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Tenant> getAllTenants() {
        return new ArrayList<>(tenancyRepository.findAll());
    }

    @Override
    public List<Tenant> getTenantsByStatus(TenantStatus status) {
        return tenancyRepository.findAll().stream()
                .filter(tenant -> tenant.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tenant> getTenantsByOwner(Long ownerId) {
        return tenancyRepository.findAll().stream()
                .filter(tenant -> tenant.getOwnerId().equals(String.valueOf(ownerId)))
                .collect(Collectors.toList());
    }

    @Override
    public List<Tenant> getTenantsByUserId(Long userId) {
        List<Long> tenantIds = getTenantIdsByUserId(userId);
        return tenancyRepository.findAllById(tenantIds);
    }

    @Override
    public List<Long> getTenantIdsByUserId(Long userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(TenantMembership::getTenantId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addUserToTenant(Long userId, Long tenantId) {
        if (!userApi.userExists(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist");
        }
        if (!tenancyRepository.existsById(tenantId)) {
            throw new IllegalArgumentException("Tenant with ID " + tenantId + " does not exist");
        }
        if (membershipRepository.existsByTenantIdAndUserId(tenantId, userId)) {
            return; // already a member; no new seat consumed
        }
        enforceSeatQuota(tenantId);
        membershipRepository.save(new TenantMembership(null, tenantId, userId, LocalDateTime.now()));
    }

    /**
     * Reject (FREE) or allow-with-overage (paid) a new seat against the tenant's
     * maxUsers allotment. -1 (or absent) means unlimited.
     */
    private void enforceSeatQuota(Long tenantId) {
        Tenant tenant = tenancyRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getSettings() == null
                || tenant.getSettings().getMaxUsers() == null
                || tenant.getSettings().getMaxUsers() < 0) {
            return; // unknown/unlimited
        }
        int limit = tenant.getSettings().getMaxUsers();
        long current = membershipRepository.countByTenantId(tenantId);
        if (current < limit) {
            return; // within allotment
        }
        if (tenant.getPlan() != TenantPlan.FREE) {
            log.warn("Tenant {} over seat allotment ({} >= {}); recording billable seat overage",
                    tenantId, current, limit);
            return;
        }
        throw new IllegalStateException(
                "Seat limit reached for tenant " + tenantId + " (" + limit
                        + "). Upgrade the plan to add more users.");
    }

    @Override
    @Transactional
    public void removeUserFromTenant(Long tenantId, Long userId) {
        membershipRepository.deleteByTenantIdAndUserId(tenantId, userId);
    }

    @Override
    @Transactional
    public void removeUserFromAllTenants(Long userId) {
        membershipRepository.deleteByUserId(userId);
    }

    @Override
    public long countUsersInTenant(Long tenantId) {
        return membershipRepository.countByTenantId(tenantId);
    }

    private TenantSettings createDefaultSettings(com.dmc.archiving.tenancy.model.TenantPlan plan) {
        return switch (plan) {
            case FREE -> new TenantSettings(5, 10, 1024L * 1024 * 100, false, false, "UTC", "en", null);
            case BASIC -> new TenantSettings(25, 100, 1024L * 1024 * 1024, true, false, "UTC", "en", null);
            case PROFESSIONAL -> new TenantSettings(100, 1000, 1024L * 1024 * 1024 * 10, true, true, "UTC", "en", null);
            case ENTERPRISE -> new TenantSettings(-1, -1, -1L, true, true, "UTC", "en", null);
            case CUSTOM -> new TenantSettings(50, 500, 1024L * 1024 * 1024 * 5, true, true, "UTC", "en", null);
        };
    }
}
