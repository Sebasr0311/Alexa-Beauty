package com.alexabeauty.security;

import com.alexabeauty.config.ProductionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationTest {
    private MockEnvironment baseProdEnv() {
        return new MockEnvironment().withProperty("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/store?sslmode=require")
                .withProperty("spring.datasource.username", "test").withProperty("spring.datasource.password", "test-only")
                .withProperty("app.jwt.secret", "test-only-random-value-of-at-least-32-characters")
                .withProperty("app.cors.allowed-origins", "https://store.example.com")
                .withProperty("app.storage.supabase.url", "https://storage.example.com")
                .withProperty("app.storage.supabase.service-role-key", "test-only")
                .withProperty("app.storage.supabase.bucket-public", "products")
                .withProperty("ADMIN_INITIAL_EMAIL", "admin@example.com")
                .withProperty("ADMIN_INITIAL_PASSWORD", "test-only-long-password");
    }

    @Test
    void rejectsTrailingSlashOriginThatBrowserOriginFilterCannotMatch() {
        var env = baseProdEnv()
                .withProperty("app.cors.allowed-origins", "https://store.example.com/")
                .withProperty("app.whatsapp-number", "573109876543");
        assertThatThrownBy(() -> new ProductionConfiguration(env).validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("FRONTEND_ORIGIN");
    }

    @Test
    void passesWhenWhatsAppNumberPropertyOmitted() {
        var env = baseProdEnv();
        assertThatCode(() -> new ProductionConfiguration(env).validate()).doesNotThrowAnyException();
    }

    @Test
    void rejectsPlaceholderOrInvalidWhatsAppNumberWhenProvided() {
        var placeholderEnv = baseProdEnv().withProperty("app.whatsapp-number", "573001234567");
        assertThatThrownBy(() -> new ProductionConfiguration(placeholderEnv).validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("WHATSAPP_BUSINESS_NUMBER");

        var invalidEnv = baseProdEnv().withProperty("app.whatsapp-number", "invalid-phone");
        assertThatThrownBy(() -> new ProductionConfiguration(invalidEnv).validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("WHATSAPP_BUSINESS_NUMBER");
    }
}
