package com.fksoft.application.auth;

import java.time.Instant;
import org.springframework.stereotype.Component;

/** Issues a login session (access token + rotating refresh) for a user — the auto-login path. */
@Component
class SessionIssuer {

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenService refreshTokens;

    SessionIssuer(AccessTokenIssuer accessTokens, RefreshTokenService refreshTokens) {
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
    }

    AuthService.AuthResult issueSession(User user, String ip, String userAgent, Instant now) {
        var access = accessTokens.issueFor(user, now);
        var refresh = refreshTokens.issueFor(user.id(), ip, userAgent, now);
        return new AuthService.AuthResult(access.token(), access.expiresAt(), refresh.rawToken(), user);
    }
}
