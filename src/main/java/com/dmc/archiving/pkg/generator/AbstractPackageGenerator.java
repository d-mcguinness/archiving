package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Shared serialisation + upload pipeline for Intake/Preservation/Release generators.
 *
 * <P> the package entity (Intake, Preservation, Release). Every entity is first
 * reduced to a {@link PackageSnapshot} via {@link #toSnapshot}, and everything
 * from there on is entity-agnostic — which is what lets the unified package
 * module reuse these generators through {@link #generate(PackageSnapshot)}.
 *
 * Subclasses implement EITHER {@link #buildPayload} (full control over bytes,
 * filename, content-type — use this for XML/ZIP/binary output) OR the
 * convenience {@link #buildPackage} which returns a JSON-serialisable Map and
 * lands at {std}_{type}.json.
 */
public abstract class AbstractPackageGenerator<P> implements PackageGenerator<P> {

    private static final Logger log = LoggerFactory.getLogger(AbstractPackageGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractPackageGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generate(P pkg) {
        return generate(toSnapshot(pkg));
    }

    @Override
    public PackagePayload render(PackageSnapshot snapshot) {
        return buildPayload(snapshot);
    }

    @Override
    public String generate(PackageSnapshot snapshot) {
        try {
            PackagePayload payload = render(snapshot);
            String fileKey = packageType() + "s/" + snapshot.id() + "/" + payload.filename();
            // Intentional (task #15): the generated artifact is uploaded to storage
            // but NOT recorded as a billable Document, so its bytes never enter the
            // storage meter. The per-package generation fee is all-in and covers the
            // artifact's storage; metering it too would double-charge the same bytes.
            // See PRICING_COGS.md §4 before changing this.
            return cloudStorageService.uploadBytes(payload.bytes(), fileKey, payload.contentType());
        } catch (Exception e) {
            log.error("Failed to generate {} package for id {}: {}",
                    packageType().toUpperCase(), snapshot.id(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate " + packageType().toUpperCase() + " package", e);
        }
    }

    /**
     * Produce the raw payload for upload. Default implementation JSON-encodes
     * the result of {@link #buildPackage} — override directly for XML/ZIP/etc.
     */
    protected PackagePayload buildPayload(PackageSnapshot snapshot) {
        Map<String, Object> data = buildPackage(snapshot);
        byte[] bytes;
        try {
            bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise package as JSON", e);
        }
        String filename = getStandardName().toLowerCase() + "_" + packageType() + ".json";
        return new PackagePayload(bytes, filename, "application/json");
    }

    /**
     * Convenience for generators that emit JSON. Either this or
     * {@link #buildPayload} must be overridden — the other can be left alone.
     */
    public Map<String, Object> buildPackage(PackageSnapshot snapshot) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " must override buildPackage() or buildPayload()");
    }

    /**
     * Resolved from {@link #getStandardName()}: a name matching an
     * {@link ArchiveStandard} constant marks a dedicated generator; any other
     * name (conventionally "DEFAULT") marks the fallback. Override to decouple
     * the two if a generator's display name ever diverges from its enum.
     */
    @Override
    public ArchiveStandard supportedStandard() {
        String name = getStandardName();
        for (ArchiveStandard s : ArchiveStandard.values()) {
            if (s.name().equals(name)) return s;
        }
        return null;
    }

    /** Convert the package entity into the stage-agnostic snapshot generators consume. */
    protected abstract PackageSnapshot toSnapshot(P pkg);
}
