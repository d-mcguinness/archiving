package com.dmc.archiving.archive.element.link;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
public class ElementLinkController {

    private final ElementLinkService linkService;

    public ElementLinkController(ElementLinkService linkService) {
        this.linkService = linkService;
    }

    @QueryMapping
    public List<ElementLink> getElementLinks(@Argument String elementId) {
        return linkService.getLinksByElement(Long.parseLong(elementId));
    }

    @QueryMapping
    public List<ElementLink> getOutgoingLinks(@Argument String elementId) {
        return linkService.getOutgoingLinks(Long.parseLong(elementId));
    }

    @QueryMapping
    public List<ElementLink> getIncomingLinks(@Argument String elementId) {
        return linkService.getIncomingLinks(Long.parseLong(elementId));
    }

    @MutationMapping
    public ElementLink createElementLink(@Argument Map<String, Object> input) {
        return linkService.createLink(
                Long.parseLong(input.get("sourceElementId").toString()),
                Long.parseLong(input.get("targetElementId").toString()),
                input.get("linkType").toString(),
                input.get("label") != null ? input.get("label").toString() : null,
                input.get("description") != null ? input.get("description").toString() : null,
                input.get("directional") != null ? Boolean.parseBoolean(input.get("directional").toString()) : true,
                input.get("createdBy") != null ? input.get("createdBy").toString() : null
        );
    }

    @MutationMapping
    public boolean deleteElementLink(@Argument String id) {
        return linkService.deleteLink(Long.parseLong(id));
    }

    @SchemaMapping(typeName = "ElementLink", field = "createdAt")
    public String createdAt(ElementLink link) {
        return link.getCreatedAt() != null
                ? link.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
    }
}