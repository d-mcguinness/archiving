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
     * Authenticate a login: return the user iff the username exists and the raw
     * password matches the stored BCrypt hash. Empty otherwise (unknown user, no
     * password set, or mismatch). Plaintext never leaves the user module.
     */
    Optional<User> authenticate(String username, String rawPassword);

    /**
     * Register a new login account (self-service signup). Validates non-blank
     * fields and a minimum password length, enforces unique username/email
     * (throws {@link IllegalArgumentException} on conflict), hashes the password,
     * and persists the user with the given role. Returns the saved user (with id).
     */
    User register(String name, String email, String username, String rawPassword, String role);

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
