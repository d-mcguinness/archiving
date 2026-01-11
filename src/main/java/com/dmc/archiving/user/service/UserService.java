package com.dmc.archiving.user.service;

import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.input.CreateUserInput;
import com.dmc.archiving.user.model.User;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

/**
 * UserService extends UserApi to provide both internal service methods
 * and expose public API methods to other modules.
 */
public interface UserService extends UserApi {

    /**
     * Get a user by ID
     * @param id the user ID
     * @return the user or null if not found
     */
    Optional<User> getUserById(Long id);

    /**
     * Get all users
     * @return list of all users
     */
    List<User> getAllUsers();

    /**
     * Create a new user
     * @param input the user creation input
     * @return the created user
     */
    User createUser(CreateUserInput input);

    /**
     * Update an existing user
     * @param id the user ID
     * @param input the updated user data
     * @return the updated user or null if not found
     */
    User updateUser(Long id, CreateUserInput input);

    /**
     * Delete a user by ID
     * @param id the user ID
     * @return true if deleted, false if not found
     */
    boolean deleteUser(Long id);

    /**
     * Check if a user exists
     * @param userId the user ID
     * @return true if user exists, false otherwise
     */
    boolean userExists(Long userId);
}
