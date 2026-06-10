package com.dmc.archiving.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "age")
    private Integer age;

    /** Login identifier. Unique; null for seeded data rows that aren't login accounts. */
    @Column(name = "username", unique = true, length = 50)
    private String username;

    /** BCrypt hash of the password; null for non-login rows. Never expose. */
    @Column(name = "password_hash")
    private String passwordHash;

    /** Account role: ADMIN / TENANT / USER. */
    @Column(name = "role", length = 20)
    private String role;

    // Note: Tenant relationship is managed from the Tenant side only
    // to maintain Spring Modulith module boundaries.
    // Use TenantService to query user's tenants if needed.
}
