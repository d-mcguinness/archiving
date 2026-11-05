package com.dmc.archiving.archive;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.model.ArchiveStatus;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Objects;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ArchiveService caches Archive entities (Caffeine). An entity cached by a request
 * that never touched its lazy {@code assignedUsers} collection used to be handed
 * to a later request that selected that field; iterating the detached collection
 * threw LazyInitializationException outside the data fetcher, so the whole
 * /graphql call failed with HTTP 500 instead of a GraphQL error. Observed on
 * /tenants/{id}/archives/{id}/update after the detail page had seeded the cache.
 *
 * Deliberately NOT @Transactional: each MockMvc call must run in its own session,
 * exactly as production requests do.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArchiveCacheLazyRelationsTest {

    @Autowired private MockMvc mvc;
    @Autowired private ArchiveRepository archives;
    @Autowired private UserRepository users;
    @Autowired private CacheManager cacheManager;

    private Long archiveId;
    private Long tenantId;
    private Long userId;

    @BeforeEach
    void seed() {
        User u = new User();
        u.setName("Cache Tester");
        u.setEmail("cache.tester." + System.nanoTime() + "@example.com");
        u.setUsername("cache" + System.nanoTime());
        u.setRole("USER");
        userId = users.save(u).getId();

        tenantId = 900_000L + System.nanoTime() % 100_000;
        Archive a = new Archive();
        a.setTenantId(tenantId);
        a.setOwnerId(userId);
        a.setTitle("Cached archive");
        a.setContent("c");
        a.setStatus(ArchiveStatus.DRAFT);
        a.setStandard(ArchiveStandard.NOARK5);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.getAssignedUsers().add(u);
        archiveId = archives.save(a).getId();

        cacheManager.getCacheNames().forEach(n -> Objects.requireNonNull(cacheManager.getCache(n)).clear());
    }

    @AfterEach
    void cleanUp() {
        archives.deleteById(archiveId);
        users.deleteById(userId);
        cacheManager.getCacheNames().forEach(n -> Objects.requireNonNull(cacheManager.getCache(n)).clear());
    }

    private static String body(String query) {
        return "{\"query\":\"" + query.replace("\"", "\\\"") + "\"}";
    }

    @Test
    void archiveCachedWithoutAssignedUsersStillServesThemLater() throws Exception {
        // 1. Seed the "archives" cache with a request that does not touch the collection
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{ getArchive(id: \"" + archiveId + "\") { id title } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.getArchive.title").value("Cached archive"));

        // 2. A later request selecting assignedUsers gets the cached, detached entity
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{ getArchive(id: \"" + archiveId + "\") { id assignedUsers { id name } } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.getArchive.assignedUsers[*].id", hasItem(userId.toString())));
    }

    @Test
    void tenantListCachedWithoutAssignedUsersStillServesThemLater() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{ getArchivesByTenant(tenantId: \"" + tenantId + "\") { id } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.getArchivesByTenant[*].id", hasItem(archiveId.toString())));

        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content(body("{ getArchivesByTenant(tenantId: \"" + tenantId + "\") { id assignedUsers { id } } }")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.data.getArchivesByTenant[0].assignedUsers[*].id", hasItem(userId.toString())));
    }
}
