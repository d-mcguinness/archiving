package com.dmc.archiving.dip.generator;

import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractDipGenerator implements DipGenerator {

    private static final Logger log = LoggerFactory.getLogger(AbstractDipGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractDipGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generate(Dip dip) {
        try {
            Map<String, Object> dipPackage = buildPackage(dip);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dipPackage);
            String fileKey = "dips/" + dip.getId() + "/" + getStandardName().toLowerCase() + "_dip.json";
            return cloudStorageService.uploadBytes(jsonBytes, fileKey, "application/json");
        } catch (Exception e) {
            log.error("Failed to generate DIP package for DIP {}: {}", dip.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate DIP package", e);
        }
    }
}
