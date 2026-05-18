package com.dmc.archiving.pkg.generator;

import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Shared serialisation + upload pipeline for SIP/AIP/DIP generators.
 *
 * <P> the package entity (Sip, Aip, Dip)
 * <S> the value passed to {@link #buildPackage(Object)} — for SIP this is a
 *     SipSnapshot; for AIP/DIP it's the entity itself.
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
            Map<String, Object> packageData = buildPackage(snapshot);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(packageData);
            String fileKey = packageType() + "s/" + packageId(pkg) + "/"
                    + getStandardName().toLowerCase() + "_" + packageType() + ".json";
            return cloudStorageService.uploadBytes(jsonBytes, fileKey, "application/json");
        } catch (Exception e) {
            log.error("Failed to generate {} package for id {}: {}",
                    packageType().toUpperCase(), packageId(pkg), e.getMessage(), e);
            throw new RuntimeException("Failed to generate " + packageType().toUpperCase() + " package", e);
        }
    }

    /** Lifecycle marker — "sip", "aip", or "dip". Drives bucket prefix + filename suffix. */
    protected abstract String packageType();

    /** Extract the package id (used in the S3 key). */
    protected abstract Long packageId(P pkg);

    /** Convert the package entity into whatever {@link #buildPackage(Object)} expects. */
    protected abstract S toSnapshot(P pkg);

    /** Build the standard-specific package payload. */
    public abstract Map<String, Object> buildPackage(S snapshot);

    /** The standard's name (e.g. "EARK") — used in the filename and for logging. */
    public abstract String getStandardName();
}
