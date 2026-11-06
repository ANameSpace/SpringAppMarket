package io.github.anamespace.appservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Storage storage = new Storage();
    private Duration downloadTtl = Duration.ofMinutes(10);
    private Duration uploadTtl = Duration.ofMinutes(10);
    private final Integrations integrations = new Integrations();

    public Storage getStorage() {
        return storage;
    }

    public Duration getDownloadTtl() {
        return downloadTtl;
    }

    public void setDownloadTtl(Duration downloadTtl) {
        this.downloadTtl = downloadTtl;
    }

    public Duration getUploadTtl() {
        return uploadTtl;
    }

    public void setUploadTtl(Duration uploadTtl) {
        this.uploadTtl = uploadTtl;
    }

    public Integrations getIntegrations() {
        return integrations;
    }

    public static class Storage {

        private String bucket = "app-market";
        private String region = "garage";
        private String accessKey;
        private String secretKey;
        private String endpoint = "http://localhost:3900";
        private String publicEndpoint = "http://localhost:3900";
        private boolean pathStyle = true;
        private long maxVersionSize = 500L * 1024 * 1024;
        private long maxImageSize = 5L * 1024 * 1024;
        /**
         * Browser origins allowed to upload/download directly through presigned
         * S3 URLs. The gateway is the public entry point, so both host spellings
         * and the local Next.js dev server are covered.
         */
        private List<String> corsAllowedOrigins = List.of(
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:3000",
                "http://127.0.0.1:3000");

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getPublicEndpoint() {
            return publicEndpoint;
        }

        public void setPublicEndpoint(String publicEndpoint) {
            this.publicEndpoint = publicEndpoint;
        }

        public boolean isPathStyle() {
            return pathStyle;
        }

        public void setPathStyle(boolean pathStyle) {
            this.pathStyle = pathStyle;
        }

        public List<String> getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }

        public long getMaxVersionSize() {
            return maxVersionSize;
        }

        public void setMaxVersionSize(long maxVersionSize) {
            this.maxVersionSize = maxVersionSize;
        }

        public long getMaxImageSize() {
            return maxImageSize;
        }

        public void setMaxImageSize(long maxImageSize) {
            this.maxImageSize = maxImageSize;
        }
    }

    public static class Integrations {

        private String orderServiceUrl = "http://order-service:8080";
        private String reviewServiceUrl = "http://review-service:8080";
        private boolean stubPurchaseCheck = true;

        public String getOrderServiceUrl() {
            return orderServiceUrl;
        }

        public void setOrderServiceUrl(String orderServiceUrl) {
            this.orderServiceUrl = orderServiceUrl;
        }

        public String getReviewServiceUrl() {
            return reviewServiceUrl;
        }

        public void setReviewServiceUrl(String reviewServiceUrl) {
            this.reviewServiceUrl = reviewServiceUrl;
        }

        public boolean isStubPurchaseCheck() {
            return stubPurchaseCheck;
        }

        public void setStubPurchaseCheck(boolean stubPurchaseCheck) {
            this.stubPurchaseCheck = stubPurchaseCheck;
        }
    }
}
