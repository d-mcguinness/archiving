package com.dmc.archiving.archive.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheme definition in an archive standard hierarchy (NOARK5 or OAIS)
 * Each scheme defines what type of children it can contain
 */
@Entity
@Table(name = "schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Scheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false)
    private ArchiveStandard standard;

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(name = "norwegian_name")
    private String norwegianName;

    @Column(name = "english_name")
    private String englishName;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // root, structural_unit, functional_entity, process, document_unit, etc.

    @Column(name = "description", length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Scheme parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scheme> children = new ArrayList<>();

    @Column(name = "is_root")
    private Boolean isRoot = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "notes", length = 2000)
    private String notes;

    // Helper methods
    public void addChild(Scheme child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Scheme child) {
        children.remove(child);
        child.setParent(null);
    }

    public boolean canContainEntityType(String entityName) {
        return children.stream()
                .anyMatch(child -> child.getEntityName().equals(entityName));
    }

    public List<String> getAllowedChildEntityNames() {
        return children.stream()
                .map(Scheme::getEntityName)
                .distinct()
                .toList();
    }
}

