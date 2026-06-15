package com.fksoft.application.api.dto;

import com.fksoft.domain.auth.AuthService;
import com.fksoft.domain.auth.UserSummaryResponse;
import java.time.Instant;

/** Login/refresh response per the SPEC-0003 contract example. */
public record AuthTokensResponse(String accessToken, Instant accessTokenExpiresAt, UserSummaryResponse user) {

    public static AuthTokensResponse from(AuthService.AuthResult result) {
        return new AuthTokensResponse(result.accessToken(), result.accessTokenExpiresAt(), result.userSummary());
    }
}
