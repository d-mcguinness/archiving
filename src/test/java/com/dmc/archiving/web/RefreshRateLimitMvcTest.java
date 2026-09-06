package com.dmc.archiving.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check that POST /api/auth/refresh is rate-limited per client IP: with
 * the cap lowered to 2, two (invalid-token → 401) attempts pass the limiter and the
 * third from the same client is 429 — fired BEFORE token processing. @TestPropertySource
 * gives this its own context so the lowered cap doesn't affect other tests. Invalid
 * tokens write nothing, so no @Transactional is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.refresh.rate-limit.max-attempts=2")
class RefreshRateLimitMvcTest {

    @Autowired private MockMvc mvc;

    private static final String BODY = "{\"refreshToken\":\"bogus-token\"}";

    @Test
    void thirdRefreshFromSameIpIsRateLimited() throws Exception {
        // First two are processed (invalid token → 401) but consume the window.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON).content(BODY))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}
