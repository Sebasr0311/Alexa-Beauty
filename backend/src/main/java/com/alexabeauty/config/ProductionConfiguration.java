package com.alexabeauty.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;

/** Fails closed before serving traffic with placeholder or insecure production settings. */
@Component
@Profile("prod")
public class ProductionConfiguration {
    private final Environment environment;
    public ProductionConfiguration(Environment environment) { this.environment = environment; }

    @PostConstruct
    public void validate() {
        String database = required("spring.datasource.url");
        if (!database.startsWith("jdbc:postgresql://") || !database.matches(".*[?&]sslmode=(require|verify-full)(&.*)?$")) {
            throw new IllegalStateException("DATABASE_URL requires PostgreSQL and sslmode=require or verify-full");
        }
        required("spring.datasource.username"); required("spring.datasource.password");
        String secret = required("app.jwt.secret");
        if (secret.length() < 32 || secret.startsWith("local-development") || secret.startsWith("404E6352")) {
            throw new IllegalStateException("JWT_SECRET must be independently generated, at least 32 bytes");
        }
        https(required("app.cors.allowed-origins"), "FRONTEND_ORIGIN");
        https(required("app.storage.supabase.url"), "SUPABASE_URL");
        required("app.storage.supabase.service-role-key"); required("app.storage.supabase.bucket-public");
        String number = required("app.whatsapp-number");
        if (!number.matches("[1-9][0-9]{9,14}") || number.equals("573001234567")) {
            throw new IllegalStateException("WHATSAPP_BUSINESS_NUMBER requires the verified real business number");
        }
        String email = required("ADMIN_INITIAL_EMAIL");
        String password = required("ADMIN_INITIAL_PASSWORD");
        if (!email.contains("@") || password.length() < 16 || password.length() > 72
                || password.contains("Cambiar123") || password.toLowerCase().contains("change-me")) {
            throw new IllegalStateException("Provide a valid ADMIN_INITIAL_EMAIL and independent 16-72 character ADMIN_INITIAL_PASSWORD");
        }
    }
    private String required(String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank() || value.contains("your-") || value.contains("[REF]") || value.contains("CHANGE_ME")) {
            throw new IllegalStateException("Missing or placeholder production configuration: " + property);
        }
        return value;
    }
    private void https(String value, String name) {
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || "localhost".equals(uri.getHost()) || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty())) {
            throw new IllegalStateException(name + " must be an exact HTTPS origin");
        }
    }
}
