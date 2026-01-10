package com.dmc.archiving.archive.entity;

import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.scheme.Scheme;
import com.dmc.archiving.archive.scheme.SchemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ElementController {

    private final ElementService elementService;
    private final ArchiveRepository archiveRepository;
    private final SchemeRepository schemeRepository;

    // Queries
    @QueryMapping
    public Element getElement(@Argument Long id) {
        return elementService.getElement(id).orElse(null);
    }

    @QueryMapping
    public List<Element> getElementsByArchive(@Argument Long archiveId) {
        return elementService.getElementsByArchive(archiveId);
    }

    @QueryMapping
    public List<Element> getRootElements(@Argument Long archiveId) {
        return elementService.getRootElements(archiveId);
    }

    @QueryMapping
    public List<Element> getElementChildren(@Argument Long elementId) {
        return elementService.getChildren(elementId);
    }

    @QueryMapping
    public Element getElementByIdentifier(@Argument Long archiveId, @Argument String elementIdentifier) {
        return elementService.getElementByIdentifier(archiveId, elementIdentifier).orElse(null);
    }

    @QueryMapping
    public List<Element> searchElements(@Argument Long archiveId, @Argument String searchTerm) {
        return elementService.searchElements(archiveId, searchTerm);
    }

    @QueryMapping
    public List<Element> getElementsByStatus(@Argument Long archiveId, @Argument String status) {
        return elementService.getElementsByStatus(archiveId, status);
    }

    @QueryMapping
    public List<Element> getElementHierarchy(@Argument Long archiveId) {
        return elementService.getHierarchy(archiveId);
    }

    @QueryMapping
    public List<Element> getLeafElements(@Argument Long archiveId) {
        return elementService.getLeafElements(archiveId);
    }

    @QueryMapping
    public Integer countElements(@Argument Long archiveId) {
        return (int) elementService.countElements(archiveId);
    }

    // Mutations
    @MutationMapping
    public Element createElement(@Argument Map<String, Object> input) {
        Long archiveId = Long.parseLong(input.get("archiveId").toString());
        Long schemeId = Long.parseLong(input.get("schemeId").toString());
        Long parentElementId = input.get("parentElementId") != null ?
            Long.parseLong(input.get("parentElementId").toString()) : null;
        String elementIdentifier = input.get("elementIdentifier").toString();
        String title = input.get("title").toString();
        String description = input.get("description") != null ? input.get("description").toString() : null;
        String createdBy = input.get("createdBy").toString();

        Archive archive = archiveRepository.findById(archiveId)
            .orElseThrow(() -> new IllegalArgumentException("Archive not found"));
        Scheme scheme = schemeRepository.findById(schemeId)
            .orElseThrow(() -> new IllegalArgumentException("Scheme not found"));
        Element parent = parentElementId != null ?
            elementService.getElement(parentElementId)
                .orElseThrow(() -> new IllegalArgumentException("Parent element not found")) : null;

        return elementService.createElement(archive, scheme, parent, elementIdentifier,
                                         title, description, createdBy);
    }

    @MutationMapping
    public Element updateElement(@Argument Long id, @Argument Map<String, Object> input) {
        String title = input.get("title").toString();
        String description = input.get("description") != null ? input.get("description").toString() : null;
        String updatedBy = input.get("updatedBy").toString();

        return elementService.updateElement(id, title, description, updatedBy);
    }

    @MutationMapping
    public Element updateElementStatus(@Argument Long id, @Argument String status, @Argument String updatedBy) {
        return elementService.updateStatus(id, status, updatedBy);
    }

    @MutationMapping
    public Element closeElement(@Argument Long id, @Argument String closedBy) {
        return elementService.closeElement(id, closedBy);
    }

    @MutationMapping
    public Boolean deleteElement(@Argument Long id) {
        elementService.deleteElement(id);
        return true;
    }

    @MutationMapping
    public Element moveElement(@Argument Long elementId, @Argument Long newParentId, @Argument String updatedBy) {
        return elementService.moveElement(elementId, newParentId, updatedBy);
    }

    // Field resolvers
    @SchemaMapping(typeName = "Element", field = "elementType")
    public String elementType(Element element) {
        return element.getElementType();
    }

    @SchemaMapping(typeName = "Element", field = "norwegianName")
    public String norwegianName(Element element) {
        return element.getNorwegianName();
    }

    @SchemaMapping(typeName = "Element", field = "englishName")
    public String englishName(Element element) {
        return element.getEnglishName();
    }

    @SchemaMapping(typeName = "Element", field = "path")
    public String path(Element element) {
        return element.getPath();
    }

    @SchemaMapping(typeName = "Element", field = "depth")
    public Integer depth(Element element) {
        return element.getDepth();
    }

    @SchemaMapping(typeName = "Element", field = "isLeaf")
    public Boolean isLeaf(Element element) {
        return element.isLeaf();
    }

    @SchemaMapping(typeName = "Element", field = "descendantsCount")
    public Integer descendantsCount(Element element) {
        return element.countDescendants();
    }
}
