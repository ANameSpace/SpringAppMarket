package io.github.anamespace.appservice.service;

import io.github.anamespace.appservice.config.AppProperties;
import io.github.anamespace.appservice.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private static final Set<String> VERSION_CONTENT_TYPES = Set.of(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-tar",
            "application/gzip",
            "application/x-gzip",
            "application/x-7z-compressed",
            "application/vnd.rar",
            "application/x-rar-compressed",
            "application/java-archive",
            "application/vnd.android.package-archive",
            "application/x-msdownload",
            "application/vnd.microsoft.portable-executable",
            "application/x-msi",
            "application/x-apple-diskimage",
            "application/octet-stream"
    );

    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties properties;
    private final String bucket;

    public StorageService(S3Client s3Client, S3Presigner s3Presigner, AppProperties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
        this.bucket = properties.getStorage().getBucket();
    }

    public void ensureBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                s3Client.createBucket(builder -> builder.bucket(bucket));
                log.info("Created S3 bucket {}", bucket);
                return;
            }
            throw exception;
        }
    }

    /**
     * Presigned PUT/GET URLs are used directly by the browser, so the bucket
     * must answer CORS preflight requests. Garage implements the S3 bucket CORS
     * API; applying the rule on startup keeps the stack self-contained.
     */
    public void ensureCors() {
        CORSConfiguration configuration = CORSConfiguration.builder()
                .corsRules(CORSRule.builder()
                        .allowedOrigins(properties.getStorage().getCorsAllowedOrigins())
                        .allowedMethods("GET", "PUT", "HEAD")
                        .allowedHeaders("*")
                        .exposeHeaders("ETag")
                        .maxAgeSeconds(3600)
                        .build())
                .build();

        s3Client.putBucketCors(PutBucketCorsRequest.builder()
                .bucket(bucket)
                .corsConfiguration(configuration)
                .build());
        log.info("Configured CORS for S3 bucket {}", bucket);
    }

    public PresignedUpload presignPut(String key, Duration ttl) {
        PresignedPutObjectRequest request = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build());

        return new PresignedUpload(request.url().toString(), key, request.expiration());
    }

    public String presignGet(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            return null;
        }
        PresignedGetObjectRequest request = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build());
        return request.url().toString();
    }

    public String presignAttachment(String key, String fileName, Duration ttl) {
        String disposition = "attachment; filename=\"" + fileName.replace("\"", "") + "\"";
        PresignedGetObjectRequest request = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .responseContentDisposition(disposition)
                        .build())
                .build());
        return request.url().toString();
    }

    public HeadObjectResponse head(String key) {
        try {
            return s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new BadRequestException("Файл не найден в хранилище");
            }
            throw exception;
        }
    }

    public String contentType(String key) {
        return head(key).contentType();
    }

    public void move(String sourceKey, String destinationKey) {
        if (sourceKey.equals(destinationKey)) {
            return;
        }
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceKey)
                .destinationBucket(bucket)
                .destinationKey(destinationKey)
                .build());
        delete(sourceKey);
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
        }
    }

    public void deletePrefix(String prefix) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();

            for (var page : s3Client.listObjectsV2Paginator(request)) {
                List<ObjectIdentifier> objects = page.contents().stream()
                        .map(object -> ObjectIdentifier.builder().key(object.key()).build())
                        .toList();
                if (!objects.isEmpty()) {
                    s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(objects).build())
                            .build());
                }
            }
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
        } catch (SdkException exception) {
            log.error("Failed to delete S3 prefix {}", prefix, exception);
            throw exception;
        }
    }

    public void validateVersionFile(String key, String prefix) {
        if (key == null || !key.startsWith(prefix)) {
            throw new BadRequestException("fileKey должен начинаться с " + prefix);
        }
        HeadObjectResponse head = head(key);
        long size = head.contentLength() == null ? 0 : head.contentLength();
        if (size <= 0) {
            throw new BadRequestException("Файл пустой");
        }
        if (size > properties.getStorage().getMaxVersionSize()) {
            throw new BadRequestException("Файл версии превышает 500 МБ");
        }
        String contentType = normalizeContentType(head.contentType());
        if (!VERSION_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Недопустимый тип файла версии: " + contentType);
        }
    }

    public String validateImageFile(String key, String prefix) {
        if (key == null || !key.startsWith(prefix)) {
            throw new BadRequestException("fileKey должен начинаться с " + prefix);
        }
        HeadObjectResponse head = head(key);
        long size = head.contentLength() == null ? 0 : head.contentLength();
        if (size <= 0) {
            throw new BadRequestException("Файл пустой");
        }
        if (size > properties.getStorage().getMaxImageSize()) {
            throw new BadRequestException("Изображение превышает 5 МБ");
        }
        String contentType = normalizeContentType(head.contentType());
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Недопустимый тип изображения: " + contentType);
        }
        return contentType;
    }

    public static String extensionForContentType(String contentType) {
        String normalized = normalizeContentType(contentType);
        return switch (normalized) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/webp" -> "webp";
            case "application/zip", "application/x-zip-compressed" -> "zip";
            case "application/x-tar" -> "tar";
            case "application/gzip", "application/x-gzip" -> "gz";
            case "application/x-7z-compressed" -> "7z";
            case "application/vnd.rar", "application/x-rar-compressed" -> "rar";
            case "application/java-archive" -> "jar";
            case "application/vnd.android.package-archive" -> "apk";
            case "application/x-msdownload", "application/vnd.microsoft.portable-executable" -> "exe";
            case "application/x-msi" -> "msi";
            case "application/x-apple-diskimage" -> "dmg";
            default -> "bin";
        };
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        int separator = contentType.indexOf(';');
        String value = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record PresignedUpload(String uploadUrl, String fileKey, Instant expiresAt) {
    }
}
