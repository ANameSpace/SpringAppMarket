package io.github.anamespace.appservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(AppProperties properties) {
        AppProperties.Storage storage = properties.getStorage();
        if (storage.getAccessKey() != null && !storage.getAccessKey().isBlank()
                && storage.getSecretKey() != null && !storage.getSecretKey().isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storage.getAccessKey(), storage.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AppProperties properties, AwsCredentialsProvider credentialsProvider) {
        AppProperties.Storage storage = properties.getStorage();
        return S3Client.builder()
                .endpointOverride(URI.create(storage.getEndpoint()))
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.isPathStyle())
                        .build())
                .build();
    }

    /**
     * Presigned URLs are handed to the browser, so they must be signed for the
     * publicly reachable S3 endpoint, not for the Docker-internal one.
     */
    @Bean
    public S3Presigner s3Presigner(AppProperties properties, AwsCredentialsProvider credentialsProvider) {
        AppProperties.Storage storage = properties.getStorage();
        return S3Presigner.builder()
                .endpointOverride(URI.create(storage.getPublicEndpoint()))
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.isPathStyle())
                        .build())
                .build();
    }
}
