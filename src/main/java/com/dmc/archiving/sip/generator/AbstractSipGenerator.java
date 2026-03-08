package com.dmc.archiving.sip.generator;

import com.dmc.archiving.sip.model.Sip;
import com.dmc.archiving.storage.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractSipGenerator implements SipGenerator {

    private static final Logger log = LoggerFactory.getLogger(AbstractSipGenerator.class);

    protected final CloudStorageService cloudStorageService;
    protected final ObjectMapper objectMapper;

    protected AbstractSipGenerator(CloudStorageService cloudStorageService) {
        this.cloudStorageService = cloudStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String generate(Sip sip) {
        try {
            Map<String, Object> sipPackage = buildPackage(sip);
            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(sipPackage);
            String fileKey = "sips/" + sip.getId() + "/" + getStandardName().toLowerCase() + "_sip.json";
            return cloudStorageService.uploadBytes(jsonBytes, fileKey, "application/json");
        } catch (Exception e) {
            log.error("Failed to generate SIP package for SIP {}: {}", sip.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate SIP package", e);
        }
    }
}
