package com.dmc.archiving.user;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The User GraphQL type exposes role, so pages can hide impersonation for admins up front. */
@SpringBootTest
@AutoConfigureMockMvc
class UserRoleGraphQlTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    private Long id;

    @AfterEach
    void cleanUp() {
        if (id != null) users.deleteById(id);
    }

    @Test
    void getUserExposesRole() throws Exception {
        User u = new User();
        u.setName("Role Probe");
        u.setEmail("role.probe." + System.nanoTime() + "@example.com");
        u.setUsername("roleprobe" + System.nanoTime());
        u.setRole("ADMIN");
        id = users.save(u).getId();

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ getUser(id: \\\"" + id + "\\\") { id role } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.getUser.role").value("ADMIN"));
    }
}
