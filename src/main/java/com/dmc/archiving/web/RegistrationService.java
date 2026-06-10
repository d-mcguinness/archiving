package com.dmc.archiving.web;

import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates self-service signup as ONE atomic unit: create the user and
 * provision their FREE-plan tenant in a single transaction. If tenant
 * provisioning fails, the user insert rolls back too — otherwise an orphan user
 * would be left whose unique username/email is permanently taken (un-retryable).
 *
 * <p>Exceptions are intentionally NOT caught here: they must propagate so the
 * declarative rollback fires. The controller maps them to HTTP status.
 */
@Service
public class RegistrationService {

    private final UserApi userApi;
    private final TenancyApi tenancyApi;

    public RegistrationService(UserApi userApi, TenancyApi tenancyApi) {
        this.userApi = userApi;
        this.tenancyApi = tenancyApi;
    }

    /** Register a new owner + their FREE tenant atomically. Returns the saved user. */
    @Transactional
    public User register(String name, String email, String username, String rawPassword, String organization) {
        // Self-service accounts are always TENANT owners; ADMIN is never granted here.
        User user = userApi.register(name, email, username, rawPassword, "TENANT");
        tenancyApi.createTenantWithOwner(organization, user.getId());
        return user;
    }
}
