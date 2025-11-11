package com.cobamovil.backend.config;

import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Optional admin seeder for environments where Flyway seeds didn't run.
 * Enable with env var APP_SEED_ADMIN=true (runs once per start, idempotent).
 */
@Configuration
public class AdminSeedRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

    @Bean
    CommandLineRunner seedAdmins(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            String flag = System.getenv("APP_SEED_ADMIN");
            if (flag == null || !flag.equalsIgnoreCase("true")) {
                return; // do nothing unless explicitly enabled
            }

            createIfMissing(users, encoder, "admin", "admin@cobamovil.com", "Admin123!");
            createIfMissing(users, encoder, "admin2", "admin2@cobamovil.com", "Admin123!");
        };
    }

    private void createIfMissing(UserRepository users, PasswordEncoder encoder,
                                 String username, String email, String rawPassword) {
        boolean exists = users.findByUsername(username).isPresent();
        if (!exists) {
            User u = new User(username, email, encoder.encode(rawPassword), "ADMIN");
            u.setAccountNonExpired(true);
            u.setAccountNonLocked(true);
            u.setCredentialsNonExpired(true);
            u.setEnabled(true);
            users.save(u);
            log.info("Seeded admin user '{}' (email: {})", username, email);
        } else {
            log.info("Admin user '{}' already exists; skipping seed", username);
        }
    }
}

