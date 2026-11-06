package io.github.anamespace.appservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Integration point with review-service. The service is still an empty
 * skeleton, so deleting reviews is a logged no-op for now.
 */
@Component
public class ReviewServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceClient.class);

    public void deleteByApp(UUID appId) {
        // TODO: DELETE {review-service}/internal/reviews/by-app/{appId}
        log.info("Stub: delete reviews for app {}", appId);
    }
}
