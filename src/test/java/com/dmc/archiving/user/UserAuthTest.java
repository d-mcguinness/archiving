package com.dmc.archiving.user;

import com.dmc.archiving.user.model.User;
import com.dmc.archiving.user.repository.UserRepository;
import com.dmc.archiving.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Verifies DB-backed credential handling end to end against a real BCrypt
 * encoder: register hashes (never stores) the password and authenticate matches
 * it; bad password / unknown user / no-hash all fail; duplicate username or email
 * and weak passwords are rejected.
 */
@DataJpaTest
class UserAuthTest {

    @Autowired private UserRepository repository;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repository, mock(ApplicationEventPublisher.class), new BCryptPasswordEncoder());
    }

    @Test
    void registerHashesPasswordAndAuthenticateMatchesIt() {
        User saved = service.register("Ada Lovelace", "ada@example.com", "ada", "password1", "TENANT");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo("TENANT");
        assertThat(saved.getPasswordHash()).isNotNull().isNotEqualTo("password1"); // stored hashed
        assertThat(saved.getPasswordHash()).startsWith("$2"); // bcrypt

        assertThat(service.authenticate("ada", "password1")).isPresent();
        assertThat(service.authenticate("ada", "wrong-password")).isEmpty();
        assertThat(service.authenticate("nobody", "password1")).isEmpty();
    }

    @Test
    void authenticateRejectsAUserWithNoPasswordHash() {
        User u = new User();
        u.setName("No Login");
        u.setEmail("nologin@example.com");
        u.setUsername("nologin"); // no passwordHash (e.g. a seeded data row)
        repository.save(u);

        assertThat(service.authenticate("nologin", "anything")).isEmpty();
    }

    @Test
    void duplicateUsernameIsRejected() {
        service.register("Ada", "ada@example.com", "ada", "password1", "TENANT");
        assertThatThrownBy(() -> service.register("Ada Two", "ada2@example.com", "ada", "password1", "TENANT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void duplicateEmailIsRejected() {
        service.register("Ada", "ada@example.com", "ada", "password1", "TENANT");
        assertThatThrownBy(() -> service.register("Ada Two", "ada@example.com", "ada2", "password1", "TENANT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void shortPasswordIsRejected() {
        assertThatThrownBy(() -> service.register("Ada", "ada@example.com", "ada", "short", "TENANT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 characters");
    }

    @Test
    void usernameWithReservedOrUnsafeCharsIsRejected() {
        // Review-PR16 CRITICAL: '_' must never be storable (it would shift the token
        // parse); also reject spaces and other unsafe characters.
        assertThatThrownBy(() -> service.register("Evil", "evil@example.com", "x_ADMIN", "password1", "TENANT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");
        assertThatThrownBy(() -> service.register("Sp", "sp@example.com", "bad name", "password1", "TENANT"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
