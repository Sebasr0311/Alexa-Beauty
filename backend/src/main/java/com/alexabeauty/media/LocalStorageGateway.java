package com.alexabeauty.media;

import com.alexabeauty.common.error.ApiException;
import com.alexabeauty.common.error.ErrorCodes;
import com.alexabeauty.common.error.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageGateway implements StorageGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageGateway.class);

    private final String localDir;
    private final String baseUrl;

    public LocalStorageGateway(
            @Value("${app.storage.local-dir:uploads}") String localDir,
            @Value("${app.storage.base-url:/uploads}") String baseUrl
    ) {
        this.localDir = localDir;
        this.baseUrl = baseUrl;
    }

    @Override
    public String upload(byte[] content, String path, String contentType) {
        try {
            Path basePath = Paths.get(localDir).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(path).normalize();

            // Prevent path traversal attacks
            if (!targetPath.startsWith(basePath)) {
                throw new ValidationException("Invalid storage path: " + path);
            }

            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }

            Files.write(targetPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Stored file locally at {}", targetPath);
            return path;
        } catch (IOException e) {
            log.error("Failed to store file locally at {}: {}", path, e.getMessage(), e);
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.INTERNAL_SERVER_ERROR,
                    "Error al guardar archivo en almacenamiento local: " + e.getMessage()
            );
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path basePath = Paths.get(localDir).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(path).normalize();

            if (targetPath.startsWith(basePath)) {
                boolean deleted = Files.deleteIfExists(targetPath);
                log.info("File deletion for {}: deleted={}", path, deleted);
            }
        } catch (IOException e) {
            log.warn("Failed to delete local file at {}: {}", path, e.getMessage());
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
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;
        String cleanBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return cleanBase + cleanPath;
    }
}
