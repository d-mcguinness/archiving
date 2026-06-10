package com.dmc.archiving.user;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides the {@link PasswordEncoder} used to hash and verify login passwords.
 * Lives in the user module so password handling stays internal to it. Uses the
 * standalone spring-security-crypto BCrypt — no Spring Security filter chain.
 */
@Configuration
public class UserSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
