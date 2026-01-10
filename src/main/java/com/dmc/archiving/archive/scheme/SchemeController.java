package com.dmc.archiving.archive.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SchemeController {

    private final SchemeService schemeService;

    @QueryMapping
    public List<Scheme> getSchemeEntities(@Argument ArchiveStandard standard) {
        return schemeService.getAllEntities(standard);
    }

    @QueryMapping
    public Scheme getSchemeEntity(@Argument ArchiveStandard standard, @Argument String entityName) {
        return schemeService.getEntity(standard, entityName).orElse(null);
    }

    @QueryMapping
    public Scheme getSchemeRootEntity(@Argument ArchiveStandard standard) {
        return schemeService.getRootEntity(standard).orElse(null);
    }


    @QueryMapping
    public List<String> getAllowedChildEntities(@Argument ArchiveStandard standard, @Argument String parentEntityName) {
        return schemeService.getAllowedChildren(standard, parentEntityName);
    }

    @QueryMapping
    public Boolean canContainEntity(@Argument ArchiveStandard standard,
                                   @Argument String parentEntityName,
                                   @Argument String childEntityName) {
        return schemeService.canContainChild(standard, parentEntityName, childEntityName);
    }

    @MutationMapping
    public Scheme createScheme(@Argument ArchiveStandard standard,
                              @Argument String entityName,
                              @Argument String norwegianName,
                              @Argument String englishName,
                              @Argument String entityType,
                              @Argument String description,
                              @Argument Boolean isRoot,
                              @Argument List<String> allowedChildren) {
        return schemeService.createScheme(standard, entityName, norwegianName, englishName,
                                         entityType, description, isRoot, allowedChildren);
    }
}

