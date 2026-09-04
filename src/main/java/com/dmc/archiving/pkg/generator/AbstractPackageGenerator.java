package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Shared serialisation + upload pipeline for SIP/AIP/DIP generators.
 *
 * <P> the package entity (Intake, Preservation, Release)
 * <S> the value passed to {@link #buildPayload} — for SIP this is a
 *     IntakeSnapshot; for AIP/DIP it's the entity itself.
 *
 * Subclasses implement EITHER {@link #buildPayload} (full control over bytes,
 * filename, content-type — use this for XML/ZIP/binary output) OR the
 * convenience {@link #buildPackage} which returns a JSON-serialisable Map and
 * lands at {std}_{type}.json.
 */
public abstract class AbstractPackageGenerator<P, S> {

    private static final Logger log = LoggerFactory.getLogger(AbstractPackageGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractPackageGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public String generate(P pkg) {
        try {
            S snapshot = toSnapshot(pkg);
            PackagePayload payload = buildPayload(snapshot);
            String fileKey = packageType() + "s/" + packageId(pkg) + "/" + payload.filename();
            // Intentional (task #15): the generated artifact is uploaded to storage
            // but NOT recorded as a billable Document, so its bytes never enter the
            // storage meter. The per-package generation fee is all-in and covers the
            // artifact's storage; metering it too would double-charge the same bytes.
            // See PRICING_COGS.md §4 before changing this.
            return cloudStorageService.uploadBytes(payload.bytes(), fileKey, payload.contentType());
        } catch (Exception e) {
            log.error("Failed to generate {} package for id {}: {}",
                    packageType().toUpperCase(), packageId(pkg), e.getMessage(), e);
            throw new RuntimeException("Failed to generate " + packageType().toUpperCase() + " package", e);
        }
    }

    /**
     * Produce the raw payload for upload. Default implementation JSON-encodes
     * the result of {@link #buildPackage} — override directly for XML/ZIP/etc.
     */
    protected PackagePayload buildPayload(S snapshot) {
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
    public Map<String, Object> buildPackage(S snapshot) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " must override buildPackage() or buildPayload()");
    }

    /** Lifecycle marker — "sip", "aip", or "dip". Drives bucket prefix + filename suffix. */
    protected abstract String packageType();

    /** Extract the package id (used in the S3 key). */
    protected abstract Long packageId(P pkg);

    /** Convert the package entity into whatever buildPayload/buildPackage expects. */
    protected abstract S toSnapshot(P pkg);

    /** The standard's name (e.g. "EARK") — used in the filename and for logging. */
    public abstract String getStandardName();
}
