package com.dmc.archiving.archive.element.field;

import com.dmc.archiving.archive.element.Element;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.modulith.NamedInterface;

@NamedInterface
@Entity
@Table(name = "fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "element_id", nullable = false)
    private Element element;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;
}
