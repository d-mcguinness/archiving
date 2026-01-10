package com.dmc.archiving.user.api;

import com.dmc.archiving.user.model.User;

/**
 * Public API for the User module.
 * This interface defines what operations are exposed to other modules.
 */
public interface UserApi {

    /**
     * Check if a user exists by ID.
     * @param userId the user ID to check
     * @return true if user exists, false otherwise
     */
    boolean userExists(Long userId);

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
