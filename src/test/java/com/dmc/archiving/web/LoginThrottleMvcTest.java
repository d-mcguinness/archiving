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
 * End-to-end check that POST /api/auth/login throttles brute force: with the
 * failure cap lowered to 3, the first three wrong-password attempts are 401 and
 * the fourth (same IP/username) is 429 — before the password is even checked.
 * @TestPropertySource gives this its own context so the lowered cap is isolated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.login.rate-limit.max-failures=3")
@Transactional
class LoginThrottleMvcTest {

    @Autowired private MockMvc mvc;

    private static final String BAD_LOGIN = "{\"username\":\"victim\",\"password\":\"wrong-password\"}";

    @Test
    void repeatedFailedLoginsAreThrottled() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(BAD_LOGIN))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}
