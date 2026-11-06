package io.github.anamespace.appservice.config;

import io.github.anamespace.appservice.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class StorageInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageInitializer.class);

    private final StorageService storageService;

    public StorageInitializer(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            storageService.ensureBucket();
            storageService.ensureCors();
        } catch (RuntimeException exception) {
            // The compose stack uses garage-init to create the bucket. A storage
            // outage must not prevent the API from starting.
            log.warn("S3 bucket is not available yet: {}", exception.getMessage());
        }
    }
}
