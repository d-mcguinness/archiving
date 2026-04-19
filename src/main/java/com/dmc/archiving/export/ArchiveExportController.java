package com.dmc.archiving.export;

import com.dmc.archiving.aip.api.AipExportApi;
import com.dmc.archiving.aip.api.AipExportFile;
import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.dip.api.DipExportApi;
import com.dmc.archiving.dip.api.DipExportFile;
import com.dmc.archiving.sip.api.SipExportApi;
import com.dmc.archiving.sip.api.SipExportFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/archives")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class ArchiveExportController {

    private static final Logger log = LoggerFactory.getLogger(ArchiveExportController.class);

    private final ArchiveExportApi archiveExportApi;
    private final SipExportApi sipExportApi;
    private final AipExportApi aipExportApi;
    private final DipExportApi dipExportApi;
    private final ObjectMapper mapper;

    public ArchiveExportController(ArchiveExportApi archiveExportApi,
                                   SipExportApi sipExportApi,
                                   AipExportApi aipExportApi,
                                   DipExportApi dipExportApi) {
        this.archiveExportApi = archiveExportApi;
        this.sipExportApi = sipExportApi;
        this.aipExportApi = aipExportApi;
        this.dipExportApi = dipExportApi;

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostMapping("/{archiveId}/export-package")
    @Transactional(readOnly = true)
    public ResponseEntity<?> exportPackage(
            @PathVariable Long archiveId,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            ArchiveExportInfo info = archiveExportApi.loadForExport(archiveId);
            if (info == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Archive not found"));
            }

            Long tenantId = info.tenantId();
            List<SipExportFile> sips = sipExportApi.exportByTenant(tenantId);
            List<AipExportFile> aips = aipExportApi.exportByTenant(tenantId);
            List<DipExportFile> dips = dipExportApi.exportByTenant(tenantId);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                Map<String, Object> manifest = new LinkedHashMap<>();
                manifest.put("exportDate", LocalDateTime.now().toString());
                manifest.put("archiveId", archiveId);
                manifest.put("archiveTitle", info.title());
                manifest.put("standard", info.standard());
                manifest.put("standardReference", info.standardReference());
                manifest.put("sipCount", sips.size());
                manifest.put("aipCount", aips.size());
                manifest.put("dipCount", dips.size());
                manifest.put("tenantId", tenantId);
                addJsonEntry(zos, "manifest.json", manifest);

                addJsonEntry(zos, "archive.json", info.data());

                for (SipExportFile sip : sips) {
                    addJsonEntry(zos, "sips/sip_" + sip.id() + "_" + sanitize(sip.title()) + ".json", sip.data());
                }
                for (AipExportFile aip : aips) {
                    addJsonEntry(zos, "aips/aip_" + aip.id() + "_" + sanitize(aip.title()) + ".json", aip.data());
                }
                for (DipExportFile dip : dips) {
                    addJsonEntry(zos, "dips/dip_" + dip.id() + "_" + sanitize(dip.title()) + ".json", dip.data());
                }
            }

            byte[] zipBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/zip"));
            headers.setContentLength(zipBytes.length);
            String zipName = "archive_" + archiveId + "_" + info.standard() + "_package.zip";
            headers.setContentDispositionFormData("attachment", zipName);

            log.info("Exported archive {} package: {} SIPs, {} AIPs, {} DIPs, {} bytes",
                    archiveId, sips.size(), aips.size(), dips.size(), zipBytes.length);

            return ResponseEntity.ok().headers(headers).body(zipBytes);

        } catch (Exception e) {
            log.error("Failed to export archive package {}: {}", archiveId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to export package: " + e.getMessage()));
        }
    }

    private void addJsonEntry(ZipOutputStream zos, String filename, Object data) throws Exception {
        zos.putNextEntry(new ZipEntry(filename));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        zos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String sanitize(String name) {
        if (name == null) return "untitled";
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
