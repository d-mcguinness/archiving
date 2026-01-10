package com.dmc.archiving.archive.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnassignUserInput {
    private Long archiveId;
    private Long userId;
}
