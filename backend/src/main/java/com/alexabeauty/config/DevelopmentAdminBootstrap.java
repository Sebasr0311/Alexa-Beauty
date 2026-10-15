package com.alexabeauty.config;

import com.alexabeauty.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures the seeded development/test admin user always has working credentials (Admin123*Alexa).
 */
@Component
@Profile("!prod")
public class DevelopmentAdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public DevelopmentAdminBootstrap(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        users.findByEmail("admin@alexabeauty.com").ifPresent(user -> {
            if (!encoder.matches("Admin123*Alexa", user.getPasswordHash())) {
                user.setPasswordHash(encoder.encode("Admin123*Alexa"));
                users.save(user);
            }
        });
    }
}
