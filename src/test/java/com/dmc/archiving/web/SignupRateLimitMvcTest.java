package com.dmc.archiving.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check that POST /api/auth/register is rate-limited per client IP: with
 * the cap lowered to 2, a third (otherwise valid, distinct) signup from the same
 * client returns 429. @TestPropertySource gives this its own context so the lowered
 * limit doesn't affect other tests. @Transactional rolls back the created users.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.signup.rate-limit.max-attempts=2")
@Transactional
class SignupRateLimitMvcTest {

    @Autowired private MockMvc mvc;

    private static String registerJson(String username, String email) {
        return "{\"name\":\"User\",\"organization\":\"Org\",\"email\":\"" + email
                + "\",\"username\":\"" + username + "\",\"password\":\"password1\"}";
    }

    @Test
    void thirdSignupFromSameIpIsRateLimited() throws Exception {
        // MockMvc requests share the default client IP, so they share one window.
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("rl1", "rl1@example.com")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("rl2", "rl2@example.com")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson("rl3", "rl3@example.com")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}
