package com.dmc.archiving.user.model;

import com.dmc.archiving.tenancy.model.Tenant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
    private Set<Tenant> tenants = new HashSet<>();
}
