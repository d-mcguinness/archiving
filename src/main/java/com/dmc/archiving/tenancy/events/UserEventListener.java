package com.dmc.archiving.tenancy.events;

import com.dmc.archiving.tenancy.service.TenancyService;
import com.dmc.archiving.user.api.UserDeletedEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to user events and performs cleanup in the tenancy module.
 */
@Component
@AllArgsConstructor
public class UserEventListener {

    private final TenancyService tenancyService;

    /**
     * When a user is deleted, remove them from all tenants first.
     * This prevents foreign key constraint violations.
     *
     * Order(0) ensures this runs before the actual deletion.
     */
    @EventListener
    @Order(0)
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        // Remove user from all tenants to prevent FK constraint violation
        tenancyService.removeUserFromAllTenants(event.getUserId());
    }
}
