package com.dmc.archiving.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end (HTTP + real beans + H2) coverage of DB-backed auth (Review-PR16
 * gap): self-service register provisions a tenant and logs in, the same
 * credentials then log in, an unsafe username is rejected (closing the ADMIN
 * parse-shift at the HTTP layer), and a duplicate username is a clean 400.
 * @Transactional rolls each test back so the shared in-memory DB stays clean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowMvcTest {

    @Autowired private MockMvc mvc;

    private static String registerJson(String username, String email) {
        return "{\"name\":\"Test User\",\"organization\":\"Test Org\",\"email\":\"" + email
                + "\",\"username\":\"" + username + "\",\"password\":\"password1\"}";
    }

    @Test
    void registerProvisionsTenantAndLogsIn_thenLoginWorks() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("ada", "ada@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tenantId").isNumber());

        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"ada\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.role").value("TENANT"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerRejectsUnsafeUsername_closingTheAdminEscalation() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("evil_ADMIN", "evil@example.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void duplicateUsernameIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("dupe", "first@example.com")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("dupe", "second@example.com")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("bob", "bob@example.com")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }
}
