package com.dmc.archiving.aip.generator;

import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractAipGenerator implements AipGenerator {

    private static final Logger log = LoggerFactory.getLogger(AbstractAipGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractAipGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generate(Aip aip) {
        try {
            Map<String, Object> aipPackage = buildPackage(aip);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(aipPackage);
            String fileKey = "aips/" + aip.getId() + "/" + getStandardName().toLowerCase() + "_aip.json";
            return cloudStorageService.uploadBytes(jsonBytes, fileKey, "application/json");
        } catch (Exception e) {
            log.error("Failed to generate AIP package for AIP {}: {}", aip.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate AIP package", e);
        }
    }
}
