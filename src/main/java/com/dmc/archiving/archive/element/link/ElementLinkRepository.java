package com.dmc.archiving.archive.element.link;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElementLinkRepository extends JpaRepository<ElementLink, Long> {

    List<ElementLink> findBySourceElementId(Long sourceElementId);

    List<ElementLink> findByTargetElementId(Long targetElementId);

    List<ElementLink> findBySourceElementIdOrTargetElementId(Long sourceId, Long targetId);

    List<ElementLink> findByLinkType(String linkType);

    List<ElementLink> findBySourceElementIdAndLinkType(Long sourceElementId, String linkType);

    boolean existsBySourceElementIdAndTargetElementIdAndLinkType(Long sourceId, Long targetId, String linkType);

    void deleteBySourceElementIdOrTargetElementId(Long sourceId, Long targetId);
}
