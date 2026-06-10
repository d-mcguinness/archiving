package com.dmc.archiving.user;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies DemoUserSeeder (Review-PR16 coverage gap): it sets a BCrypt hash for a
 * seeded demo account that has none (so admin/tenant/user can log in), is
 * idempotent (won't overwrite an existing hash), and silently skips demo
 * accounts that aren't present.
 */
@DataJpaTest
class DemoUserSeederTest {

    @Autowired private UserRepository repository;

    private DemoUserSeeder seeder() {
        return new DemoUserSeeder(repository, new BCryptPasswordEncoder());
    }

    private User demoUser(String username, String passwordHash) {
        User u = new User();
        u.setName("Demo " + username);
        u.setEmail(username + "@example.com");
        u.setUsername(username);
        u.setRole("ADMIN");
        u.setPasswordHash(passwordHash);
        return repository.save(u);
    }

    @Test
    void setsHashForADemoAccountThatHasNone() throws Exception {
        demoUser("admin", null);

        seeder().run();

        String hash = repository.findByUsername("admin").orElseThrow().getPasswordHash();
        assertThat(hash).isNotNull().startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches("admin123", hash)).isTrue();
    }

    @Test
    void isIdempotentAndDoesNotOverwriteAnExistingHash() throws Exception {
        String preset = new BCryptPasswordEncoder().encode("something-else");
        demoUser("admin", preset);

        seeder().run();

        assertThat(repository.findByUsername("admin").orElseThrow().getPasswordHash()).isEqualTo(preset);
    }

    @Test
    void silentlySkipsDemoAccountsThatAreNotSeeded() throws Exception {
        seeder().run(); // no demo users in the DB — must not throw
        assertThat(repository.findByUsername("admin")).isEmpty();
    }
}
