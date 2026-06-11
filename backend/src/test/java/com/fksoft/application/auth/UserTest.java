package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserTest {

    @Test
    void normalizesEmailToLowercaseAtStorage() {
        var user = new User("  USER@Example.COM ", "hash", "Frank", Role.CUSTOMER);
        assertThat(user.email()).isEqualTo("user@example.com");
    }

    @Test
    void requireActiveThrowsForDisabledUsers() {
        var user = new User("user@example.com", "hash", "Frank", Role.CUSTOMER);
        user.disable();
        assertThatThrownBy(user::requireActive).isInstanceOf(UserDisabledException.class);
    }

    @Test
    void bcryptCost12RoundTrip() {
        var encoder = new BCryptPasswordEncoder(12);
        var hash = encoder.encode("secret123");
        assertThat(hash).startsWith("$2a$12$");
        assertThat(encoder.matches("secret123", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }
}
