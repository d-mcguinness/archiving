package com.dmc.archiving.archive.element;

import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.web.BaseGraphQlController;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.springframework.graphql.data.method.annotation.Argument;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Controller
public class ElementController extends BaseGraphQlController {

    private final ElementService elementService;
    private final ArchiveRepository archiveRepository;

    public ElementController(ElementService elementService, ArchiveRepository archiveRepository, TenancyApi tenancyApi) {
        super(tenancyApi);
        this.elementService = elementService;
        this.archiveRepository = archiveRepository;
    }

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
    public Element addChildElement(@Argument Long parentElementId, @Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        Element parent = elementService.getElement(parentElementId)
            .orElseThrow(() -> new IllegalArgumentException("Parent element not found"));

        String elementIdentifier = input.get("elementIdentifier").toString();
        String entityName = input.get("entityName").toString();
        String entityType = input.get("entityType").toString();
        String norwegianName = input.get("norwegianName") != null ? input.get("norwegianName").toString() : null;
        String englishName = input.get("englishName") != null ? input.get("englishName").toString() : null;
        String title = input.get("title").toString();
        String description = input.get("description") != null ? input.get("description").toString() : null;
        String createdBy = input.get("createdBy").toString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldsInput = input.get("fields") != null ?
            (List<Map<String, Object>>) input.get("fields") : List.of();

        return elementService.createElement(parent.getArchive(), parent,
            elementIdentifier, entityName, entityType, norwegianName, englishName,
            title, description, createdBy, fieldsInput);
    }

    @MutationMapping
    public Element createElement(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        Long archiveId = Long.parseLong(input.get("archiveId").toString());
        Long parentElementId = input.get("parentElementId") != null ?
            Long.parseLong(input.get("parentElementId").toString()) : null;
        String elementIdentifier = input.get("elementIdentifier").toString();
        String entityName = input.get("entityName").toString();
        String entityType = input.get("entityType").toString();
        String norwegianName = input.get("norwegianName") != null ? input.get("norwegianName").toString() : null;
        String englishName = input.get("englishName") != null ? input.get("englishName").toString() : null;
        String title = input.get("title").toString();
        String description = input.get("description") != null ? input.get("description").toString() : null;
        String createdBy = input.get("createdBy").toString();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fieldsInput = input.get("fields") != null ?
            (List<Map<String, Object>>) input.get("fields") : List.of();

        Archive archive = archiveRepository.findById(archiveId)
            .orElseThrow(() -> new IllegalArgumentException("Archive not found"));
        Element parent = parentElementId != null ?
            elementService.getElement(parentElementId)
                .orElseThrow(() -> new IllegalArgumentException("Parent element not found")) : null;

        return elementService.createElement(archive, parent, elementIdentifier, entityName, entityType,
                                         norwegianName, englishName, title, description, createdBy, fieldsInput);
    }

    @MutationMapping
    public Element updateElement(@Argument Long id, @Argument Map<String, Object> input, @Argument List<Map<String, Object>> fields, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        String title = input.get("title").toString();
        String description = input.get("description") != null ? input.get("description").toString() : null;
        String updatedBy = input.get("updatedBy").toString();

        return elementService.updateElement(id, title, description, updatedBy, fields);
    }

    @MutationMapping
    public Element updateElementStatus(@Argument Long id, @Argument String status, @Argument String updatedBy, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return elementService.updateStatus(id, status, updatedBy);
    }

    @MutationMapping
    public Element closeElement(@Argument Long id, @Argument String closedBy, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return elementService.closeElement(id, closedBy);
    }

    @MutationMapping
    public Boolean deleteElement(@Argument Long id, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        elementService.deleteElement(id);
        return true;
    }

    @MutationMapping
    public Element moveElement(@Argument Long elementId, @Argument Long newParentId, @Argument String updatedBy, DataFetchingEnvironment env) {
        requireRole(env, "TENANT", "ADMIN");
        return elementService.moveElement(elementId, newParentId, updatedBy);
    }

    // Field resolvers

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
