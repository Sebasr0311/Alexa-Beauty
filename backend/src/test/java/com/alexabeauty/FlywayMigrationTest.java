package com.alexabeauty;

import com.alexabeauty.auth.domain.Role;
import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.config.domain.AppSetting;
import com.alexabeauty.config.repository.AppSettingRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify all Flyway migrations are successfully executed and applied")
    void verifyFlywayMigrationsApplied() {
        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertThat(appliedMigrations).hasSize(6);

        assertThat(appliedMigrations[0].getVersion().getVersion()).isEqualTo("1");
        assertThat(appliedMigrations[0].getDescription()).isEqualTo("initial schema");

        assertThat(appliedMigrations[1].getVersion().getVersion()).isEqualTo("2");
        assertThat(appliedMigrations[1].getDescription()).isEqualTo("inventory and orders");

        assertThat(appliedMigrations[2].getVersion().getVersion()).isEqualTo("3");
        assertThat(appliedMigrations[2].getDescription()).isEqualTo("seed data");

        assertThat(appliedMigrations[5].getVersion().getVersion()).isEqualTo("6");
        assertThat(appliedMigrations[5].getDescription()).isEqualTo("stock receipt idempotency");
    }

    @Test
    @DisplayName("Verify initial app settings seeded by V3 migration")
    void verifyInitialAppSettingsSeeded() {
        Optional<AppSetting> businessName = appSettingRepository.findBySettingKey("business_name");
        assertThat(businessName).isPresent();
        assertThat(businessName.get().getSettingValue()).isEqualTo("Alexa Beauty");

        Optional<AppSetting> currency = appSettingRepository.findBySettingKey("currency");
        assertThat(currency).isPresent();
        assertThat(currency.get().getSettingValue()).isEqualTo("COP");

        Optional<AppSetting> timezone = appSettingRepository.findBySettingKey("timezone");
        assertThat(timezone).isPresent();
        assertThat(timezone.get().getSettingValue()).isEqualTo("America/Bogota");

        Optional<AppSetting> reservationMinutes = appSettingRepository.findBySettingKey("reservation_minutes");
        assertThat(reservationMinutes).isPresent();
        assertThat(reservationMinutes.get().getSettingValue()).isEqualTo("45");

        Optional<AppSetting> whatsappNumber = appSettingRepository.findBySettingKey("whatsapp_number");
        assertThat(whatsappNumber).isPresent();
        assertThat(whatsappNumber.get().getSettingValue()).isEqualTo("573001234567");
    }

    @Test
    @DisplayName("Verify initial categories seeded by V3 migration")
    void verifyInitialCategoriesSeeded() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        assertThat(categories).hasSize(4);
        assertThat(categories)
                .extracting(Category::getSlug)
                .containsExactly("rostro", "labios", "ojos", "accesorios");
    }

    @Test
    @DisplayName("Verify initial brands seeded by V3 migration")
    void verifyInitialBrandsSeeded() {
        List<Brand> brands = brandRepository.findByActiveTrue();
        assertThat(brands).hasSize(3);
        assertThat(brands)
                .extracting(Brand::getName)
                .containsExactlyInAnyOrder("BELLA COSMETICS", "ROSA GLOW", "LUNA BEAUTY");
    }

    @Test
    @DisplayName("Verify initial administrator seeded by V3 migration")
    void verifyInitialAdminSeeded() {
        Optional<User> admin = userRepository.findByEmail("admin@alexabeauty.com");
        assertThat(admin).isPresent();
        assertThat(admin.get().getFullName()).isEqualTo("Administrador Alexa Beauty");
        assertThat(admin.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.get().isActive()).isTrue();
    }
}
