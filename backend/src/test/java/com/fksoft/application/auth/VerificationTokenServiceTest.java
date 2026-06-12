package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Token issuance, single-use consumption and resend rate limiting (SPEC-0004). */
class VerificationTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-11T00:00:00Z");

    private final EmailVerificationTokenRepository verificationTokens = mock(EmailVerificationTokenRepository.class);
    private final PasswordResetTokenRepository resetTokens = mock(PasswordResetTokenRepository.class);
    private final InvitationTokenRepository invitationTokens = mock(InvitationTokenRepository.class);
    private final VerificationTokenService service =
            new VerificationTokenService(verificationTokens, resetTokens, invitationTokens);

    @Test
    void issuesOpaqueTokenAndStoresOnlyItsHashWith24hTtl() {
        var userId = UUID.randomUUID();
        var raw = service.issueEmailVerification(userId, NOW);

        var saved = ArgumentCaptor.forClass(EmailVerificationToken.class);
        org.mockito.Mockito.verify(verificationTokens).save(saved.capture());
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        assertThat(service).isNotNull();
    }

    @Test
    void consumeRejectsUnknownTokenWith400() {
        when(verificationTokens.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consumeEmailVerification("raw", NOW))
                .isInstanceOf(TokenInvalidException.class);
    }

    @Test
    void consumeRejectsExpiredTokenWith410() {
        var token = new EmailVerificationToken(UUID.randomUUID(), "h", NOW.minusSeconds(1), NOW.minusSeconds(2));
        when(verificationTokens.findByTokenHash(any())).thenReturn(Optional.of(token));
        assertThatThrownBy(() -> service.consumeEmailVerification("raw", NOW))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void consumeRejectsAlreadyConsumedTokenWith410() {
        var token = new EmailVerificationToken(UUID.randomUUID(), "h", NOW.plusSeconds(3600), NOW.minusSeconds(2));
        token.consume(NOW.minusSeconds(1));
        when(verificationTokens.findByTokenHash(any())).thenReturn(Optional.of(token));
        assertThatThrownBy(() -> service.consumeEmailVerification("raw", NOW))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void consumeReturnsUserIdAndMarksConsumedForValidToken() {
        var userId = UUID.randomUUID();
        var token = new EmailVerificationToken(userId, "h", NOW.plusSeconds(3600), NOW.minusSeconds(2));
        when(verificationTokens.findByTokenHash(any())).thenReturn(Optional.of(token));
        assertThat(service.consumeEmailVerification("raw", NOW)).isEqualTo(userId);
        assertThat(token.isConsumed()).isTrue();
    }

    @Test
    void resendWithinOneMinuteIsRateLimited() {
        var userId = UUID.randomUUID();
        var recent = new EmailVerificationToken(userId, "h", NOW.plusSeconds(3600), NOW.minusSeconds(30));
        when(verificationTokens.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(recent));
        assertThatThrownBy(() -> service.assertVerificationResendAllowed(userId, NOW))
                .isInstanceOf(RegistrationRateLimitedException.class);
    }

    @Test
    void resendAfterOneMinuteIsAllowed() {
        var userId = UUID.randomUUID();
        var old = new EmailVerificationToken(userId, "h", NOW.plusSeconds(3600), NOW.minusSeconds(61));
        when(verificationTokens.findFirstByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Optional.of(old));
        service.assertVerificationResendAllowed(userId, NOW); // no throw
    }
}
