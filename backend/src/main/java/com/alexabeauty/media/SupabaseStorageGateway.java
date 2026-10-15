package com.alexabeauty.media;

import com.alexabeauty.common.error.ApiException;
import com.alexabeauty.common.error.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "supabase")
public class SupabaseStorageGateway implements StorageGateway {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageGateway.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final RestClient restClient;

    public SupabaseStorageGateway(
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.storage.supabase.bucket-public:products}") String bucket
    ) {
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.bucket = bucket;
        this.restClient = RestClient.builder().build();
    }

    @PostConstruct
    public void init() {
        if (supabaseUrl != null && !supabaseUrl.isBlank() && serviceRoleKey != null && !serviceRoleKey.isBlank()) {
            ensureBucketExists();
        }
    }

    public boolean ensureBucketExists() {
        try {
            String baseUrl = cleanBaseUrl(this.supabaseUrl);
            String endpoint = String.format("%s/storage/v1/bucket", baseUrl);
            restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"id\":\"" + bucket + "\",\"name\":\"" + bucket + "\",\"public\":true}")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully ensured public Supabase storage bucket: '{}'", bucket);
            return true;
        } catch (Exception e) {
            log.info("Supabase storage bucket '{}' verification: {}", bucket, e.getMessage());
            return false;
        }
    }

    private void executeUpload(byte[] content, String endpoint, String contentType) {
        restClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String upload(byte[] content, String path, String contentType) {
        String cleanPath = sanitizePath(path);
        String baseUrl = cleanBaseUrl(this.supabaseUrl);
        String endpoint = String.format("%s/storage/v1/object/%s/%s", baseUrl, bucket, cleanPath);

        try {
            executeUpload(content, endpoint, contentType);
            log.info("Uploaded object to Supabase storage: {}/{}", bucket, cleanPath);
            return cleanPath;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("NoSuchBucket")) {
                log.warn("Bucket '{}' not found in Supabase. Attempting automatic provisioning...", bucket);
                if (ensureBucketExists()) {
                    try {
                        executeUpload(content, endpoint, contentType);
                        log.info("Uploaded object to Supabase storage after bucket provisioning: {}/{}", bucket, cleanPath);
                        return cleanPath;
                    } catch (Exception retryEx) {
                        log.error("Retry upload to Supabase storage failed: {}", retryEx.getMessage(), retryEx);
                    }
                }
            }
            log.error("Failed to upload object to Supabase storage at {}: {}", endpoint, e.getMessage(), e);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_SERVER_ERROR,
                    "Error al subir imagen a Supabase Storage: " + e.getMessage()
            );
        }
    }

    @Override
    public void delete(String path) {
        String cleanPath = sanitizePath(path);
        String baseUrl = cleanBaseUrl(this.supabaseUrl);
        String endpoint = String.format("%s/storage/v1/object/%s/%s", baseUrl, bucket, cleanPath);

        try {
            restClient.delete()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted object from Supabase storage: {}/{}", bucket, cleanPath);
        } catch (Exception e) {
            log.warn("Failed to delete object from Supabase storage at {}: {}", endpoint, e.getMessage());
        }
    }

    @Override
    public String getPublicUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String cleanPath = sanitizePath(path);
        String baseUrl = cleanBaseUrl(this.supabaseUrl);
        return String.format("%s/storage/v1/object/public/%s/%s", baseUrl, bucket, cleanPath);
    }

    private String sanitizePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String cleanBaseUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
