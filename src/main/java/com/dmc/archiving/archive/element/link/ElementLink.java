package com.dmc.archiving.archive.element.link;

import com.dmc.archiving.archive.element.Element;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a cross-reference link between two elements.
 * Supports:
 * - NOARK5 kryssreferanse (cross-references between mappe/registrering)
 * - PREMIS event-object-agent relationships
 * - METS structMap-to-fileSec pointers
 * - Any standard-specific element-to-element association
 */
@Entity
@Table(name = "element_links", indexes = {
    @Index(name = "idx_el_link_source", columnList = "source_element_id"),
    @Index(name = "idx_el_link_target", columnList = "target_element_id"),
    @Index(name = "idx_el_link_type", columnList = "link_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ElementLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_element_id", nullable = false)
    private Element sourceElement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_element_id", nullable = false)
    private Element targetElement;

    /**
     * Type of link. Examples:
     * - "crossReference" (NOARK5 kryssreferanse)
     * - "event" (PREMIS event linking object and agent)
     * - "structMapPointer" (METS structMap to fileSec)
     * - "derivedFrom" (AIP derived from SIP element)
     * - "relatedTo" (generic association)
     */
    @Column(name = "link_type", nullable = false, length = 50)
    private String linkType;

    /** Optional label describing the relationship */
    @Column(name = "label", length = 255)
    private String label;

    /** Optional description with more detail */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Directional: if true, link is source→target only; if false, bidirectional */
    @Column(name = "directional", nullable = false)
    private Boolean directional = true;

    /** Optional metadata as JSON for standard-specific link properties */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
