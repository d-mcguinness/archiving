package com.dmc.archiving.archive.dto;

import com.dmc.archiving.archive.model.Archive;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * GraphQL-compatible wrapper for paginated Archive results
 */
public record ArchivePage(
        List<Archive> content,
        PageInfo pageInfo,
        boolean empty
) {
    public static ArchivePage from(Page<Archive> page) {
        return new ArchivePage(
                page.getContent(),
                PageInfo.from(page),
                page.isEmpty()
        );
    }

    public record PageInfo(
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious,
            boolean isFirst,
            boolean isLast
    ) {
        public static PageInfo from(Page<?> page) {
            return new PageInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext(),
                    page.hasPrevious(),
                    page.isFirst(),
                    page.isLast()
            );
        }
    }
}

