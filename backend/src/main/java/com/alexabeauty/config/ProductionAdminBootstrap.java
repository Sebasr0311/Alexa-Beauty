package com.alexabeauty.config;

import com.alexabeauty.auth.domain.Role;
import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.config.domain.AppSetting;
import com.alexabeauty.config.repository.AppSettingRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Replaces seeded development credentials once, never resets a configured account on restart. */
@Component
@Profile("prod")
public class ProductionAdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final AppSettingRepository settings;
    private final PasswordEncoder encoder;
    private final Environment environment;
    public ProductionAdminBootstrap(UserRepository users, AppSettingRepository settings,
                                    PasswordEncoder encoder, Environment environment) {
        this.users = users; this.settings = settings; this.encoder = encoder; this.environment = environment;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        // Lock the stable seeded setting row so replicas cannot bootstrap twice.
        settings.lockBootstrapSetting();
        if (settings.findBySettingKey("production_admin_bootstrapped").isPresent()) return;
        String email = environment.getRequiredProperty("ADMIN_INITIAL_EMAIL").toLowerCase().trim();
        users.findByEmail("admin@alexabeauty.com").ifPresent(user -> {
            user.setActive(false); users.save(user);
        });
        User user = users.findByEmail(email).orElseGet(() -> User.builder().email(email).build());
        user.setRole(Role.ADMIN); user.setFullName("Administrator"); user.setActive(true);
        user.setPasswordHash(encoder.encode(environment.getRequiredProperty("ADMIN_INITIAL_PASSWORD")));
        users.save(user);
        settings.save(AppSetting.builder().settingKey("production_admin_bootstrapped")
                .settingValue("true").description("Production administrator initialized; no automatic reset").build());
    }
}
