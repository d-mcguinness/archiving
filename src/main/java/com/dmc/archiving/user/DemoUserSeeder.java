package com.dmc.archiving.user;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Sets BCrypt password hashes for the seeded demo accounts (admin/tenant/user)
 * at startup, so they can log in via the DB-backed flow. Idempotent — only sets a
 * hash when missing. Runs after data.sql has annotated those users with their
 * usernames. NOT active under the {@code prod} profile: production must provision
 * real accounts, not the public demo passwords.
 */
@Component
@Profile("!prod")
public class DemoUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    /** Demo username -> demo password. Mirrors the old hardcoded DEFAULT_CREDENTIALS. */
    private static final Map<String, String> DEMO_PASSWORDS = Map.of(
            "admin", "admin123",
            "tenant", "tenant123",
            "user", "user123");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        DEMO_PASSWORDS.forEach((username, rawPassword) -> {
            Optional<User> existing = userRepository.findByUsername(username);
            if (existing.isEmpty()) {
                return; // demo user not seeded (e.g. data.sql disabled) — nothing to do
            }
            User user = existing.get();
            if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Seeded demo password for account '{}'", username);
            }
        });
    }
}
