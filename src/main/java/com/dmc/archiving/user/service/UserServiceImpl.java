package com.dmc.archiving.user.service;

import com.dmc.archiving.user.api.CreateUserRequest;
import com.dmc.archiving.user.api.UserDeletedEvent;
import com.dmc.archiving.user.input.CreateUserInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username)
                .filter(u -> u.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }

    @Override
    public User register(String name, String email, String username, String rawPassword, String role) {
        if (isBlank(name) || isBlank(email) || isBlank(username) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("Name, email, username and password are required");
        }
        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User createUser(CreateUserInput input) {
        User user = new User();
        user.setName(input.getName());
        user.setEmail(input.getEmail());
        user.setAge(input.getAge());
        return userRepository.save(user);
    }

    @Override
    public User updateUser(Long id, CreateUserInput input) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return null;
        }
        user.setName(input.getName());
        user.setEmail(input.getEmail());
        user.setAge(input.getAge());
        return userRepository.save(user);
    }

    @Override
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        // Publish event to notify other modules that user is being deleted
        // This allows other modules (like tenancy) to clean up associations
        // before the actual deletion happens
        eventPublisher.publishEvent(new UserDeletedEvent(this, id));

        userRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    // UserApi implementation methods (adapter methods)

    @Override
    public Long createUser(CreateUserRequest request) {
        // Convert CreateUserRequest to CreateUserInput
        CreateUserInput input = new CreateUserInput();
        input.setName(request.getName());
        input.setEmail(request.getEmail());
        input.setAge(request.getAge());

        User user = createUser(input);
        return user.getId();
    }

    @Override
    public User updateUser(Long id, CreateUserRequest input) {
        // Convert CreateUserRequest to CreateUserInput
        CreateUserInput updateInput = new CreateUserInput();
        updateInput.setName(input.getName());
        updateInput.setEmail(input.getEmail());
        updateInput.setAge(input.getAge());

        return updateUser(id, updateInput);
    }

}
