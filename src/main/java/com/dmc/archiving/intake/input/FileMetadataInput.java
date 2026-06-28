package com.dmc.archiving.intake.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataInput {

    private String filename;
    private String contentType;
    private Long fileSize;
    private String checksum;
    private String uploadedAt;
    private String uploaderName;
    private int fileCount;
}
