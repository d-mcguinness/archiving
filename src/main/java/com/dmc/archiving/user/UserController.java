package com.dmc.archiving.user;

import com.dmc.archiving.common.BaseGraphQlController;
import com.dmc.archiving.tenancy.service.TenancyService;
import com.dmc.archiving.user.input.CreateUserInput;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class UserController extends BaseGraphQlController {

    private final UserService userService;

    public UserController(UserService userService, TenancyService tenancyService) {
        super(tenancyService);
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
    public User createUser(@Argument CreateUserInput input) {
        return userService.createUser(input);
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument CreateUserInput input) {
        return userService.updateUser(id, input);
    }

    @MutationMapping
    public boolean deleteUser(@Argument Long id) {
        return userService.deleteUser(id);
    }
}
