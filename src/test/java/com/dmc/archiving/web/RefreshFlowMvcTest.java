package com.dmc.archiving.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end (HTTP + real beans + H2) coverage of refresh-token rotation: a
 * fresh signup hands back a refresh token, exchanging it at /api/auth/refresh
 * rotates to a NEW access + refresh token, the consumed refresh token is then
 * rejected (single-use + reuse detection), logout actually revokes, and an
 * unknown token is a clean 401. Precise expiry timing is covered by the unit
 * test (H2 wall-clock can't be advanced); this nails the wiring + status codes.
 *
 * <p>Deliberately NOT @Transactional (mirrors RegisterAtomicityTest): rotation
 * is inherently about state surviving ACROSS separate HTTP requests/transactions
 * — joining every request into one rolled-back test tx (with open-session-in-view
 * handing each request its own EntityManager) would hide the consumed token's
 * revoke from the next request and test the wrong thing. Each method uses unique
 * usernames so the committed rows don't collide; the in-memory DB is dropped at
 * suite end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RefreshFlowMvcTest {

    @Autowired private MockMvc mvc;

    private static String registerJson(String username, String email) {
        return "{\"name\":\"Test User\",\"organization\":\"Test Org\",\"email\":\"" + email
                + "\",\"username\":\"" + username + "\",\"password\":\"password1\"}";
    }

    private static String refreshJson(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    /** Register and return the issued refresh token. */
    private String registerAndGetRefreshToken(String username, String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(registerJson(username, email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    @Test
    void refreshRotatesAndTheConsumedTokenIsRejected() throws Exception {
        String first = registerAndGetRefreshToken("rota", "rota@example.com");

        String refreshed = mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON)
                        .content(refreshJson(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String second = JsonPath.read(refreshed, "$.refreshToken");
        assertThat(second).isNotEqualTo(first); // rotated

        // Re-presenting the consumed first token is rejected (single-use + reuse).
        mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON).content(refreshJson(first)))
                .andExpect(status().isUnauthorized());
        // And reuse-detection revoked the whole chain, so the successor dies too.
        mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON).content(refreshJson(second)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String token = registerAndGetRefreshToken("bye", "bye@example.com");

        mvc.perform(post("/api/auth/logout").contentType(APPLICATION_JSON).content(refreshJson(token)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON).content(refreshJson(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownRefreshTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON)
                        .content(refreshJson("never-issued-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
