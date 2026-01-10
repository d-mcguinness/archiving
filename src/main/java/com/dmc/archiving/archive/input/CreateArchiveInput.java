package com.dmc.archiving.archive.input;

import com.dmc.archiving.archive.model.ArchiveStandard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateArchiveInput {
    private Long userId;
    private String title;
    private String description;
    private String content;
    private ArchiveStandard standard;
}
