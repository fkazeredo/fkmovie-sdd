package com.fksoft.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenServiceTest {

    private static final Duration TTL = Duration.ofDays(7);

    private final RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
    private final RefreshTokenService service = new RefreshTokenService(repository, TTL);

    @Test
    void issuesOpaque256BitTokenAndStoresOnlyItsHash() {
        var userId = UUID.randomUUID();
        var now = Instant.now();

        var issued = service.issueFor(userId, "1.2.3.4", "agent", now);

        var saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(saved.capture());
        assertThat(issued.rawToken()).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(saved.getValue().tokenHash()).isEqualTo(RefreshTokenService.sha256Hex(issued.rawToken()));
        assertThat(saved.getValue().expiresAt()).isEqualTo(now.plus(TTL));
        assertThat(saved.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void rotationRevokesTheOldTokenAndLinksItsReplacement() {
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var existing = new RefreshToken(userId, "hash", now.plus(TTL), now.minusSeconds(60), "ip", "agent");
        when(repository.findForUpdateByTokenHash(RefreshTokenService.sha256Hex("raw")))
                .thenReturn(Optional.of(existing));

        var result = service.rotate("raw", "1.2.3.4", "agent", now);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.replacedById()).isEqualTo(result.issued().token().id());
    }

    @Test
    void reusingARevokedTokenRevokesEveryTokenOfTheUser() {
        var userId = UUID.randomUUID();
        var now = Instant.now();
        var revoked = new RefreshToken(userId, "hash", now.plus(TTL), now.minusSeconds(60), "ip", "agent");
        revoked.revoke(now.minusSeconds(30));
        when(repository.findForUpdateByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotate("raw", "ip", "agent", now))
                .isInstanceOf(TokenReuseDetectedException.class);
        verify(repository).revokeAllForUser(userId, now);
        verify(repository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        var now = Instant.now();
        var expired =
                new RefreshToken(UUID.randomUUID(), "hash", now.minusSeconds(1), now.minusSeconds(60), "ip", "agent");
        when(repository.findForUpdateByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("raw", "ip", "agent", now))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void unknownTokenIsRejected() {
        when(repository.findForUpdateByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("raw", "ip", "agent", Instant.now()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
