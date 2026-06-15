package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
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
    void newCustomerStartsActiveUnverifiedWithGivenLocale() {
        var user = User.newCustomer("c@example.com", "hash", "Frank", "en");
        assertThat(user.role()).isEqualTo(Role.CUSTOMER);
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.preferredLocale()).isEqualTo("en");
    }

    @Test
    void verifyEmailIsIdempotentAndKeepsFirstTime() {
        var user = User.newCustomer("c@example.com", "hash", "Frank", "pt-BR");
        var first = java.time.Instant.parse("2026-06-11T00:00:00Z");
        user.verifyEmail(first);
        user.verifyEmail(first.plusSeconds(60));
        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void invitedUserHasNoPasswordIsDisabledAndUnverified() {
        var user = User.invited("op@example.com", "Op", Role.OPERATOR, java.util.UUID.randomUUID(), Instant.now());
        assertThat(user.role()).isEqualTo(Role.OPERATOR);
        assertThat(user.status()).isEqualTo(UserStatus.DISABLED);
        assertThat(user.passwordHash()).isNull();
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void acceptInvitationSetsPasswordActivatesAndVerifies() {
        var user = User.invited("op@example.com", "Op", Role.OPERATOR, null, Instant.now());
        user.acceptInvitation("hash", "Op Silva", Instant.parse("2026-06-12T00:00:00Z"));
        assertThat(user.passwordHash()).isEqualTo("hash");
        assertThat(user.name()).isEqualTo("Op Silva");
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isEmailVerified()).isTrue();
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
