package com.dmc.archiving.archive.element.link;

import com.dmc.archiving.archive.element.ElementService;
import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.AuthGuard;
import com.dmc.archiving.tenancy.api.TenancyApi;
import graphql.schema.DataFetchingEnvironment;
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
    private final ElementService elementService;
    private final TenancyApi tenancyApi;

    public ElementLinkController(ElementLinkService linkService, ElementService elementService,
                                 TenancyApi tenancyApi) {
        this.linkService = linkService;
        this.elementService = elementService;
        this.tenancyApi = tenancyApi;
    }

    /** A TENANT/USER may only touch a link whose element is in a tenant they belong to (ADMIN bypasses). */
    private void requireElementTenant(DataFetchingEnvironment env, Long elementId) {
        AuthContext ctx = AuthGuard.context(env);
        if (ctx.isAdmin()) {
            return;
        }
        Long tenantId = elementService.getArchiveTenantId(elementId);
        if (tenantId == null || !tenancyApi.isUserInTenant(ctx.userId(), tenantId)) {
            throw new AccessDeniedException("Access denied: not a member of the element's tenant");
        }
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
    public ElementLink createElementLink(@Argument Map<String, Object> input, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "TENANT", "ADMIN");
        Long sourceElementId = Long.parseLong(input.get("sourceElementId").toString());
        Long targetElementId = Long.parseLong(input.get("targetElementId").toString());
        // Both endpoints must be in a tenant the caller belongs to.
        requireElementTenant(env, sourceElementId);
        requireElementTenant(env, targetElementId);
        return linkService.createLink(
                sourceElementId,
                targetElementId,
                input.get("linkType").toString(),
                input.get("label") != null ? input.get("label").toString() : null,
                input.get("description") != null ? input.get("description").toString() : null,
                input.get("directional") != null ? Boolean.parseBoolean(input.get("directional").toString()) : true,
                input.get("createdBy") != null ? input.get("createdBy").toString() : null
        );
    }

    @MutationMapping
    public boolean deleteElementLink(@Argument String id, DataFetchingEnvironment env) {
        AuthGuard.requireRole(env, "TENANT", "ADMIN");
        ElementLink link = linkService.getLink(Long.parseLong(id));
        if (link == null) {
            throw new IllegalArgumentException("Element link not found: " + id);
        }
        // The link belongs to the tenant of its (source) element.
        requireElementTenant(env, link.getSourceElement().getId());
        return linkService.deleteLink(Long.parseLong(id));
    }

    @SchemaMapping(typeName = "ElementLink", field = "createdAt")
    public String createdAt(ElementLink link) {
        return link.getCreatedAt() != null
                ? link.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;
    }
}