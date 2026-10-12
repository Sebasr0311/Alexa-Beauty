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

    @Override
    public String upload(byte[] content, String path, String contentType) {
        String cleanPath = sanitizePath(path);
        String baseUrl = cleanBaseUrl(this.supabaseUrl);
        String endpoint = String.format("%s/storage/v1/object/%s/%s", baseUrl, bucket, cleanPath);

        try {
            restClient.post()
                    .uri(endpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Uploaded object to Supabase storage: {}/{}", bucket, cleanPath);
            return cleanPath;
        } catch (Exception e) {
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
