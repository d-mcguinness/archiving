package com.dmc.archiving.tenancy.service;

import com.dmc.archiving.tenancy.input.CreateTenantInput;
import com.dmc.archiving.tenancy.input.UpdateTenantInput;
import com.dmc.archiving.tenancy.model.Tenant;
import com.dmc.archiving.tenancy.model.TenantStatus;
import com.dmc.archiving.tenancy.model.TenantSettings;
import com.dmc.archiving.tenancy.repository.TenancyRepository;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class TenancyServiceImpl implements TenancyService {
    private final TenancyRepository tenancyRepository;
    private final UserApi userApi;

    @Override
    public Tenant createTenant(CreateTenantInput input) {
        // Validate that owner exists - convert String to Long for userApi call
        Long ownerIdLong = Long.valueOf(input.getOwnerId());
        if (!userApi.userExists(ownerIdLong)) {
            throw new IllegalArgumentException("Owner user with ID " + input.getOwnerId() + " does not exist");
        }

        // Validate domain uniqueness
        if (tenancyRepository.findByDomain(input.getDomain()).isPresent()) {
            throw new IllegalArgumentException("Tenant with domain " + input.getDomain() + " already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        TenantSettings defaultSettings = createDefaultSettings(input.getPlan());

        Tenant tenant = new Tenant(
            null, // Let JPA generate the ID
            input.getName(),
            input.getDomain(),
            input.getDisplayName(),
            input.getDescription(),
            TenantStatus.ACTIVE,
            input.getPlan(),
            now,
            now,
            String.valueOf(input.getOwnerId()), // Convert ownerId to String
            defaultSettings,
            new HashSet<>() // Initialize empty users set
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
            // Update settings based on new plan
            tenant.setSettings(createDefaultSettings(input.getPlan()));
        }

        tenant.setUpdatedAt(LocalDateTime.now());
        tenancyRepository.save(tenant);

        return tenant;
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
        return tenancyRepository.findById(tenantId)
            .map(tenant -> tenant.getUsers().stream()
                .anyMatch(user -> user.getId().equals(userId)))
            .orElse(false);
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
    public void addUserToTenant(Long userId, Long tenantId) {
        if (!userApi.userExists(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist");
        }

        Tenant tenant = tenancyRepository.findById(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant with ID " + tenantId + " does not exist"));

        User user = userApi.getUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User with ID " + userId + " does not exist"));

        tenant.getUsers().add(user);
        tenancyRepository.save(tenant);
    }

    @Override
    public void removeUserFromTenant(Long userId) {
        // Find all tenants and remove the user from them
        List<Tenant> allTenants = tenancyRepository.findAll();
        for (Tenant tenant : allTenants) {
            tenant.getUsers().removeIf(user -> user.getId().equals(userId));
            tenancyRepository.save(tenant);
        }
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
