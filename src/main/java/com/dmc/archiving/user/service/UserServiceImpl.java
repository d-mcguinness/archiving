package com.dmc.archiving.user.service;

import com.dmc.archiving.user.api.CreateUserRequest;
import com.dmc.archiving.user.input.CreateUserInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

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
