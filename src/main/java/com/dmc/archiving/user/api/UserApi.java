package com.dmc.archiving.user.api;

import com.dmc.archiving.user.model.User;

import java.util.Optional;

/**
 * Public API for the User module.
 * This is the ONLY interface exposed to other modules.
 * All cross-module user operations must go through this API.
 */
public interface UserApi {

    /**
     * Check if a user exists by ID.
     * Used by archive and tenancy modules for validation.
     *
     * @param userId the user ID to check
     * @return true if user exists, false otherwise
     */
    boolean userExists(Long userId);

    /**
     * Get a user by ID.
     * Used by tenancy module for user-tenant assignment.
     *
     * @param userId the user ID
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> getUserById(Long userId);

    /**
     * Create a new user.
     * @param request the user creation request
     * @return the created user's ID
     */
    Long createUser(CreateUserRequest request);

    /**
     * Update an existing user.
     * @param id the user ID
     * @param input the new user data
     * @return the updated user
     */
    User updateUser(Long id, CreateUserRequest input);

    /**
     * Delete a user by ID.
     * @param id the user ID
     * @return true if deleted, false otherwise
     */
    boolean deleteUser(Long id);
}
