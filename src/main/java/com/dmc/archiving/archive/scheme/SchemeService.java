package com.dmc.archiving.archive.scheme;

import com.dmc.archiving.archive.model.ArchiveStandard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;

    /**
     * Get the root scheme for a standard
     */
    public Optional<Scheme> getRootEntity(ArchiveStandard standard) {
        return schemeRepository.findByStandardAndIsRootTrue(standard);
    }

    /**
     * Get all schemes for a standard
     */
    public List<Scheme> getAllEntities(ArchiveStandard standard) {
        return schemeRepository.findByStandard(standard);
    }

    /**
     * Get scheme by standard and name
     */
    public Optional<Scheme> getEntity(ArchiveStandard standard, String entityName) {
        return schemeRepository.findByStandardAndEntityName(standard, entityName);
    }

    /**
     * Get children of a scheme
     */
    public List<Scheme> getChildren(Long entityId) {
        return schemeRepository.findByParentId(entityId);
    }

    /**
     * Get full hierarchy for a standard
     */
    public List<Scheme> getHierarchy(ArchiveStandard standard) {
        return schemeRepository.findHierarchyByStandard(standard);
    }

    /**
     * Check if a scheme can contain a specific child type
     */
    public boolean canContainChild(ArchiveStandard standard, String parentEntityName, String childEntityName) {
        Optional<Scheme> parent = getEntity(standard, parentEntityName);
        if (parent.isEmpty()) {
            return false;
        }
        return parent.get().canContainEntityType(childEntityName);
    }

    /**
     * Get allowed child entity names for a parent
     */
    public List<String> getAllowedChildren(ArchiveStandard standard, String parentEntityName) {
        Optional<Scheme> parent = getEntity(standard, parentEntityName);
        return parent.map(Scheme::getAllowedChildEntityNames).orElse(List.of());
    }

    /**
     * Create a new scheme
     */
    @Transactional
    public Scheme createEntity(Scheme entity) {
        return schemeRepository.save(entity);
    }

    /**
     * Create a scheme from parameters
     */
    @Transactional
    public Scheme createScheme(ArchiveStandard standard,
                              String entityName,
                              String norwegianName,
                              String englishName,
                              String entityType,
                              String description,
                              Boolean isRoot,
                              List<String> allowedChildren) {
        Scheme scheme = new Scheme();
        scheme.setStandard(standard);
        scheme.setEntityName(entityName);
        scheme.setNorwegianName(norwegianName);
        scheme.setEnglishName(englishName);
        scheme.setEntityType(entityType);
        scheme.setDescription(description);
        scheme.setIsRoot(isRoot != null ? isRoot : false);

        // Note: allowedChildren will be used by the frontend for validation
        // The actual parent-child relationships are managed through the parent field
        // This is just for initial creation - children relationships can be added later

        return schemeRepository.save(scheme);
    }
}
