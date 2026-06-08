package com.dmc.archiving.user;

import com.dmc.archiving.auth.api.AuthGuard;
import com.dmc.archiving.user.input.CreateUserInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.service.UserService;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public User getUser(@Argument Long id) {
        return userService.getUserById(id).orElseThrow();
    }

    @MutationMapping
    public User createUser(@Argument CreateUserInput input, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return userService.createUser(input);
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument CreateUserInput input, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return userService.updateUser(id, input);
    }

    @MutationMapping
    public boolean deleteUser(@Argument Long id, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "ADMIN");
        return userService.deleteUser(id);
    }
}
