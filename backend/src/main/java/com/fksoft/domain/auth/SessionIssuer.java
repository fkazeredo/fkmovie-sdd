package com.fksoft.domain.auth;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Issues a login session (access token + rotating refresh) for a user — the auto-login path. */
@Component
@RequiredArgsConstructor
class SessionIssuer {

    private final AccessTokens accessTokens;
    private final RefreshTokenService refreshTokens;

    AuthService.AuthResult issueSession(User user, String ip, String userAgent, Instant now) {
        var access = accessTokens.issue(user.id().toString(), user.role().name(), user.tenantId(), now);
        var refresh = refreshTokens.issueFor(user.id(), ip, userAgent, now);
        return new AuthService.AuthResult(access.token(), access.expiresAt(), refresh.rawToken(), user);
    }
}
