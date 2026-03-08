package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.model.Archive;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.modulith.NamedInterface;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an actual instance of an element in an archive structure
 * (e.g., an actual "arkiv", "mappe", "registrering" instance)
 */
@NamedInterface
@Entity
@Table(name = "elements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Element {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id")
    private Archive archive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_element_id")
    private Element parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Element> children = new ArrayList<>();

    @OneToMany(mappedBy = "element", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Field> fields = new ArrayList<>();

    @Column(name = "element_identifier", nullable = false, length = 255)
    private String elementIdentifier;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "norwegian_name", length = 255)
    private String norwegianName;

    @Column(name = "english_name", length = 255)
    private String englishName;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by", length = 255)
    private String closedBy;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // e.g., "Opprettet", "Under behandling", "Avsluttet"

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON field for flexible storage of entity-specific metadata

    @Column(name = "is_root")
    private Boolean isRoot = false;

    // Helper methods
    public void addChild(Element child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Element child) {
        if (children != null) {
            children.remove(child);
            child.setParent(null);
        }
    }

    public void addField(Field field) {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        fields.add(field);
        field.setElement(this);
    }

    public void removeField(Field field) {
        if (fields != null) {
            fields.remove(field);
            field.setElement(null);
        }
    }


    /**
     * Get the full path of this element in the hierarchy
     */
    public String getPath() {
        if (parent == null) {
            return "/" + elementIdentifier;
        }
        return parent.getPath() + "/" + elementIdentifier;
    }

    /**
     * Get depth in the hierarchy (root = 0)
     */
    public int getDepth() {
        if (parent == null) {
            return 0;
        }
        return parent.getDepth() + 1;
    }

    /**
     * Check if this element is a leaf (has no children)
     */
    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

    /**
     * Count total descendants (recursively)
     */
    public int countDescendants() {
        if (children == null || children.isEmpty()) {
            return 0;
        }
        int count = children.size();
        for (Element child : children) {
            count += child.countDescendants();
        }
        return count;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "Opprettet";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
