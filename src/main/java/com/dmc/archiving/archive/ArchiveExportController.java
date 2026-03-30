package com.dmc.archiving.archive;

import com.dmc.archiving.archive.element.Element;
import com.dmc.archiving.archive.element.field.Field;
import com.dmc.archiving.archive.element.link.ElementLink;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.ArchiveStrategy;
import com.dmc.archiving.archive.strategy.ArchiveStrategyFactory;
import com.dmc.archiving.sip.SipService;
import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.aip.AipService;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.dip.DipService;
import com.dmc.archiving.dip.model.Dip;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * REST controller for exporting archives as ZIP packages
 * containing archive metadata + all associated SIPs, AIPs, DIPs
 */
@RestController
@RequestMapping("/api/archives")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:4173", "http://localhost:5173"})
public class ArchiveExportController {

    private static final Logger log = LoggerFactory.getLogger(ArchiveExportController.class);

    private final ArchiveService archiveService;
    private final ArchiveStrategyFactory strategyFactory;
    private final SipService sipService;
    private final AipService aipService;
    private final DipService dipService;
    private final ObjectMapper mapper;

    public ArchiveExportController(ArchiveService archiveService,
                                   ArchiveStrategyFactory strategyFactory,
                                   SipService sipService,
                                   AipService aipService,
                                   DipService dipService) {
        this.archiveService = archiveService;
        this.strategyFactory = strategyFactory;
        this.sipService = sipService;
        this.aipService = aipService;
        this.dipService = dipService;

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Export a full archive as a ZIP package.
     * Contains: archive.json, sips/*.json, aips/*.json, dips/*.json
     */
    @PostMapping("/{archiveId}/export-package")
    @Transactional(readOnly = true)
    public ResponseEntity<?> exportPackage(
            @PathVariable Long archiveId,
            @RequestBody(required = false) Map<String, String> request) {

        try {
            Archive archive = archiveService.getArchiveForExport(archiveId);
            if (archive == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Archive not found"));
            }

            Long tenantId = archive.getTenantId();

            // Export archive using strategy
            ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());
            Map<String, Object> archiveData = strategy.export(archive);

            // Load all SIPs, AIPs, DIPs for this tenant
            List<Sip> sips = sipService.getSipsByTenant(tenantId);
            List<Aip> aips = aipService.getAipsByTenant(tenantId);
            List<Dip> dips = dipService.getDipsByTenant(tenantId);

            // Build ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                // Manifest
                Map<String, Object> manifest = new LinkedHashMap<>();
                manifest.put("exportDate", LocalDateTime.now().toString());
                manifest.put("archiveId", archiveId);
                manifest.put("archiveTitle", archive.getTitle());
                manifest.put("standard", archive.getStandard().name());
                manifest.put("standardReference", strategy.getStandardName());
                manifest.put("sipCount", sips.size());
                manifest.put("aipCount", aips.size());
                manifest.put("dipCount", dips.size());
                manifest.put("tenantId", tenantId);
                addJsonEntry(zos, "manifest.json", manifest);

                // Archive
                addJsonEntry(zos, "archive.json", archiveData);

                // SIPs
                for (Sip sip : sips) {
                    Map<String, Object> sipData = exportPackageEntity(sip.getId(), sip.getTitle(),
                            sip.getDescription(), sip.getStandard().name(), sip.getStatus().name(),
                            sip.getOwnerId(), sip.getTenantId(), sip.getCreatedAt(), sip.getUpdatedAt(),
                            sip.getRootElement(), "SIP", null, null);
                    String filename = "sips/sip_" + sip.getId() + "_" + sanitize(sip.getTitle()) + ".json";
                    addJsonEntry(zos, filename, sipData);
                }

                // AIPs
                for (Aip aip : aips) {
                    Map<String, Object> aipData = exportPackageEntity(aip.getId(), aip.getTitle(),
                            aip.getDescription(), aip.getStandard().name(), aip.getStatus().name(),
                            aip.getOwnerId(), aip.getTenantId(), aip.getCreatedAt(), aip.getUpdatedAt(),
                            aip.getRootElement(), "AIP", aip.getSourceSipId(), null);
                    String filename = "aips/aip_" + aip.getId() + "_" + sanitize(aip.getTitle()) + ".json";
                    addJsonEntry(zos, filename, aipData);
                }

                // DIPs
                for (Dip dip : dips) {
                    Map<String, Object> dipData = exportPackageEntity(dip.getId(), dip.getTitle(),
                            dip.getDescription(), dip.getStandard().name(), dip.getStatus().name(),
                            dip.getOwnerId(), dip.getTenantId(), dip.getCreatedAt(), dip.getUpdatedAt(),
                            dip.getRootElement(), "DIP", null, dip.getSourceAipId());
                    String filename = "dips/dip_" + dip.getId() + "_" + sanitize(dip.getTitle()) + ".json";
                    addJsonEntry(zos, filename, dipData);
                }
            }

            byte[] zipBytes = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("application/zip"));
            headers.setContentLength(zipBytes.length);
            String zipName = "archive_" + archiveId + "_" + archive.getStandard().name() + "_package.zip";
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

    private Map<String, Object> exportPackageEntity(Long id, String title, String description,
                                                      String standard, String status,
                                                      Long ownerId, Long tenantId,
                                                      LocalDateTime createdAt, LocalDateTime updatedAt,
                                                      Element rootElement, String stage,
                                                      Long sourceSipId, Long sourceAipId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("stage", stage);
        data.put("title", title);
        data.put("description", description);
        data.put("standard", standard);
        data.put("status", status);
        data.put("ownerId", ownerId);
        data.put("tenantId", tenantId);
        data.put("createdAt", createdAt);
        data.put("updatedAt", updatedAt);

        if (sourceSipId != null) data.put("sourceSipId", sourceSipId);
        if (sourceAipId != null) data.put("sourceAipId", sourceAipId);

        if (rootElement != null) {
            data.put("rootElement", exportElement(rootElement));
        }

        return data;
    }

    private Map<String, Object> exportElement(Element element) {
        Map<String, Object> elementData = new LinkedHashMap<>();
        elementData.put("id", element.getId());
        elementData.put("elementIdentifier", element.getElementIdentifier());
        elementData.put("entityName", element.getEntityName());
        elementData.put("entityType", element.getEntityType());
        elementData.put("title", element.getTitle());
        if (element.getDescription() != null) elementData.put("description", element.getDescription());
        if (element.getCreatedAt() != null) elementData.put("createdAt", element.getCreatedAt());
        if (element.getCreatedBy() != null) elementData.put("createdBy", element.getCreatedBy());

        if (element.getFields() != null && !element.getFields().isEmpty()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            for (Field field : element.getFields()) {
                if (field.getValue() != null && !field.getValue().isEmpty()) {
                    fields.put(field.getName(), Map.of(
                            "label", field.getLabel() != null ? field.getLabel() : field.getName(),
                            "type", field.getType() != null ? field.getType() : "text",
                            "value", field.getValue()
                    ));
                }
            }
            if (!fields.isEmpty()) elementData.put("fields", fields);
        }

        if (element.getOutgoingLinks() != null && !element.getOutgoingLinks().isEmpty()) {
            List<Map<String, Object>> links = new ArrayList<>();
            for (ElementLink link : element.getOutgoingLinks()) {
                Map<String, Object> linkData = new LinkedHashMap<>();
                linkData.put("linkType", link.getLinkType());
                linkData.put("targetElementId", link.getTargetElement().getId());
                linkData.put("targetEntityName", link.getTargetElement().getEntityName());
                if (link.getLabel() != null) linkData.put("label", link.getLabel());
                links.add(linkData);
            }
            elementData.put("links", links);
        }

        if (element.getChildren() != null && !element.getChildren().isEmpty()) {
            List<Map<String, Object>> children = new ArrayList<>();
            for (Element child : element.getChildren()) {
                children.add(exportElement(child));
            }
            elementData.put("children", children);
        }

        return elementData;
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