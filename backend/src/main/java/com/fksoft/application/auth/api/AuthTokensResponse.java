package com.fksoft.application.auth.api;

import com.fksoft.application.auth.AuthService;
import java.time.Instant;

/** Login/refresh response per the SPEC-0003 contract example. */
public record AuthTokensResponse(String accessToken, Instant accessTokenExpiresAt, UserSummaryResponse user) {

    static AuthTokensResponse from(AuthService.AuthResult result) {
        return new AuthTokensResponse(
                result.accessToken(), result.accessTokenExpiresAt(), UserSummaryResponse.from(result.user()));
    }
}
