package com.dmc.archiving.sip.prefill;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.sip.input.FileMetadataInput;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SipPrefillService {

    public Map<String, String> prefillFields(ArchiveStandard standard, FileMetadataInput meta) {
        String id = generateId(meta);
        String name = stripExtension(meta.getFilename());
        String date = meta.getUploadedAt() != null ? meta.getUploadedAt() : today();
        String user = meta.getUploaderName() != null ? meta.getUploaderName() : "System";
        String size = meta.getFileSize() != null ? meta.getFileSize().toString() : "0";
        String type = meta.getContentType() != null ? meta.getContentType() : "application/octet-stream";
        int count = meta.getFileCount() > 0 ? meta.getFileCount() : 1;

        return switch (standard) {
            case NOARK5 -> noark5(id, name, date, user, type);
            case OAIS -> oais(id, name, date, user, type, size, count);
            case PREMIS -> premis(id, name, date, meta.getChecksum());
            case DUBLIN_CORE -> dublinCore(id, name, type);
            case METS -> mets(id, name, type);
            case EAD -> ead(id);
            case BAGIT -> bagit(name, meta.getFileSize(), count);
            case ISADG -> isadg(id, count);
            case MODS -> mods(id);
            case EARK -> eark(id, name, date, user, type, count);
        };
    }

    private Map<String, String> noark5(String id, String name, String date, String user, String type) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("systemID", id);
        m.put("title", name);
        m.put("description", "");
        m.put("archiveStatus", "Created");
        m.put("documentMedium", "Electronic archive");
        m.put("storageLocation", "Default storage");
        m.put("createdDate", date);
        m.put("createdBy", user);
        m.put("closedDate", date);
        m.put("closedBy", user);
        return m;
    }

    private Map<String, String> oais(String id, String name, String date, String user, String type, String size, int count) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("packageID", id);
        m.put("title", name);
        m.put("description", "");
        m.put("submissionDate", date);
        m.put("producer", user);
        m.put("producerContact", "");
        m.put("submissionAgreementRef", "SA-" + id);
        m.put("packageType", "SIP");
        m.put("contentInformationType", mapContentType(type));
        m.put("completeness", "Complete");
        m.put("numberOfObjects", String.valueOf(count));
        m.put("totalSize", size);
        return m;
    }

    private Map<String, String> premis(String id, String name, String date, String checksum) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("objectIdentifierType", "SHA-256");
        m.put("objectIdentifierValue", checksum != null ? checksum : id);
        m.put("objectCategory", "File");
        m.put("preservationLevelType", "full");
        m.put("preservationLevelValue", "full preservation");
        m.put("preservationLevelRole", "requirement");
        m.put("preservationLevelRationale", "Default preservation policy");
        m.put("preservationLevelDateAssigned", date);
        m.put("significantPropertiesType", "content");
        m.put("significantPropertiesValue", "All content preserved");
        m.put("originalName", name);
        return m;
    }

    private Map<String, String> dublinCore(String id, String name, String type) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("resourceIdentifier", id);
        m.put("resourceType", mapContentType(type));
        return m;
    }

    private Map<String, String> mets(String id, String name, String type) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("metsID", id);
        m.put("objID", "OBJ-" + id);
        m.put("label", name);
        m.put("type", "digital object");
        m.put("profile", "http://www.loc.gov/standards/mets/profiles");
        return m;
    }

    private Map<String, String> ead(String id) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("eadID", id);
        m.put("audience", "external");
        m.put("relatedEncoding", "Dublin Core");
        m.put("lang", "eng");
        m.put("script", "Latn");
        m.put("base", "");
        return m;
    }

    private Map<String, String> bagit(String name, Long fileSize, int count) {
        String bagName = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        long size = fileSize != null ? fileSize : 0L;
        Map<String, String> m = new LinkedHashMap<>();
        m.put("bagName", bagName);
        m.put("payloadOxum", size + "." + count);
        m.put("bagSize", humanReadableSize(size));
        m.put("isComplete", "true");
        m.put("isValid", "true");
        return m;
    }

    private Map<String, String> isadg(String id, int count) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("descriptionID", id);
        m.put("levelOfDescription", count > 1 ? "File" : "Item");
        return m;
    }

    private Map<String, String> mods(String id) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("modsID", id);
        m.put("version", "3.8");
        return m;
    }

    private Map<String, String> eark(String id, String name, String date, String user, String type, int count) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("packageID", id);
        m.put("title", name);
        m.put("description", "");
        m.put("profile", "https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml");
        m.put("contentInformationType", mapContentType(type));
        m.put("oaisPackageType", "SIP");
        m.put("creationDate", date);
        m.put("creator", user);
        m.put("preservationLevel", "full");
        m.put("representationCount", String.valueOf(count));
        return m;
    }

    // --- helpers ---

    private String generateId(FileMetadataInput meta) {
        String fragment = UUID.randomUUID().toString().substring(0, 8);
        return "SIP-" + fragment;
    }

    private String stripExtension(String filename) {
        if (filename == null) return "Untitled";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String today() {
        return java.time.LocalDate.now().toString();
    }

    private String mapContentType(String contentType) {
        if (contentType == null) return "Digital";
        if (contentType.startsWith("image/")) return "Image";
        if (contentType.startsWith("video/")) return "Video";
        if (contentType.startsWith("audio/")) return "Audio";
        if (contentType.startsWith("text/")) return "Text";
        if (contentType.contains("pdf")) return "Document";
        if (contentType.contains("xml")) return "Structured Data";
        if (contentType.contains("json")) return "Structured Data";
        if (contentType.contains("spreadsheet") || contentType.contains("excel")) return "Dataset";
        return "Digital";
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
