package com.fksoft.domain.auth;

import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator at startup so the admin API is reachable (SPEC-0005). Runs
 * only when {@code BOOTSTRAP_ADMIN_EMAIL}/{@code BOOTSTRAP_ADMIN_PASSWORD} are set and no
 * admin exists yet — idempotent, so the env vars are ignored after the first admin is created.
 */
@Component
@Slf4j
class BootstrapAdmin implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    BootstrapAdmin(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin-email:}") String email,
            @Value("${app.bootstrap.admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || users.existsByRole(Role.ADMIN)) {
            return;
        }
        var admin = new User(email, passwordEncoder.encode(password), "Administrator", Role.ADMIN);
        admin.verifyEmail(Instant.now());
        users.save(admin);
        log.info("bootstrap admin created email={}", User.normalizeEmail(email));
    }
}
