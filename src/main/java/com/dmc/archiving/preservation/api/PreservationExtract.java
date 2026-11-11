package com.dmc.archiving.preservation.api;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.pkg.generator.PackagePayload;

import java.util.Map;

/**
 * Everything a cross-module extract of one preservation needs: the generic
 * description ({@code data}, as in the tenant export), the standard-conformant
 * artifact the package generator renders for it, and the links that lead back
 * to the originating archive.
 */
public record PreservationExtract(
        Long id,
        String title,
        Long tenantId,
        ArchiveStandard standard,
        Long sourceIntakeId,
        Map<String, Object> data,
        PackagePayload artifact
) {}
