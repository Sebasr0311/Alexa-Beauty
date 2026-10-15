package com.alexabeauty.operations.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Health", description = "Public system liveness and health diagnostics")
public class HealthController {

    private final String appName;
    private final String version;

    public HealthController(
            @Value("${spring.application.name:Alexa Beauty API}") String appName,
            @Value("${app.version:1.0.0}") String version) {
        // Human-friendly title fallback
        this.appName = appName.equals("alexa-beauty-api") ? "Alexa Beauty API" : appName;
        this.version = version;
    }

    @GetMapping({"/api/v1/health", "/api/v1/health/public", "/health"})
    @Operation(summary = "Public health check", description = "Returns basic liveness and version info without authentication")
    public ResponseEntity<PublicHealthResponse> getPublicHealth() {
        return ResponseEntity.ok(new PublicHealthResponse(
            "UP",
            appName,
            version,
            Instant.now()
        ));
    }

    public record PublicHealthResponse(
        String status,
        String appName,
        String version,
        Instant timestamp
    ) {}
}
