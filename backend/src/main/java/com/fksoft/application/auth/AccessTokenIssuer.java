package com.fksoft.application.auth;

import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues HS256 access tokens with the claims defined by ADR 0005 / SPEC-0003:
 * {@code sub} = userId, single {@code role}, {@code tenantId}, {@code iat}/{@code exp}.
 */
@Component
class AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTtl;

    AccessTokenIssuer(JwtEncoder jwtEncoder, @Value("${app.jwt.access-ttl}") Duration accessTtl) {
        this.jwtEncoder = jwtEncoder;
        this.accessTtl = accessTtl;
    }

    IssuedAccessToken issueFor(User user, Instant now) {
        var expiresAt = now.plus(accessTtl);
        var claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .claim("role", user.role().name())
                .claim("tenantId", user.tenantId())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }

    record IssuedAccessToken(String token, Instant expiresAt) {}
}
