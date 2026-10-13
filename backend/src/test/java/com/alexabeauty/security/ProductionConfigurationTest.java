package com.alexabeauty.security;

import com.alexabeauty.config.ProductionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationTest {
    @Test
    void rejectsTrailingSlashOriginThatBrowserOriginFilterCannotMatch() {
        var env = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/store?sslmode=require")
                .withProperty("spring.datasource.username", "test").withProperty("spring.datasource.password", "test-only")
                .withProperty("app.jwt.secret", "test-only-random-value-of-at-least-32-characters")
                .withProperty("app.cors.allowed-origins", "https://store.example.com/")
                .withProperty("app.storage.supabase.url", "https://storage.example.com")
                .withProperty("app.storage.supabase.service-role-key", "test-only")
                .withProperty("app.storage.supabase.bucket-public", "products")
                .withProperty("app.whatsapp-number", "573109876543")
                .withProperty("ADMIN_INITIAL_EMAIL", "admin@example.com")
                .withProperty("ADMIN_INITIAL_PASSWORD", "test-only-long-password");
        assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FRONTEND_ORIGIN");
    }
}
