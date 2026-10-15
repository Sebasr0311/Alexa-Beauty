package com.alexabeauty.operations.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint ultraligero de latido y keep-alive para monitores externos (ej. UptimeRobot, Render).
 */
@RestController
@Tag(name = "Ping", description = "Lightweight keep-alive and ping endpoints")
public class PingController {

    @GetMapping({"/api/v1/ping", "/ping"})
    @Operation(summary = "Lightweight heartbeat", description = "Returns instant status for uptime pingers without querying database")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Alexa Beauty API",
            "timestamp", Instant.now().toString()
        ));
    }
}
