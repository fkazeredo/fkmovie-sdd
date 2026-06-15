package com.fksoft.infra.security;

import com.fksoft.application.auth.AccessTokens;
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
 * HS256 access-token adapter (ADR 0005 / SPEC-0003): the technical implementation of the auth
 * module's {@link AccessTokens} port. Encodes the claims {@code sub}=subject, single {@code role},
 * {@code tenantId} and {@code iat}/{@code exp}. Lives in infra so all JWT machinery (encoder,
 * decoder, security chain) sits together; the auth domain depends only on the port.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokens {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTtl;

    JwtAccessTokenIssuer(JwtEncoder jwtEncoder, @Value("${app.jwt.access-ttl}") Duration accessTtl) {
        this.jwtEncoder = jwtEncoder;
        this.accessTtl = accessTtl;
    }

    @Override
    public IssuedAccessToken issue(String subject, String role, String tenantId, Instant now) {
        var expiresAt = now.plus(accessTtl);
        var claims = JwtClaimsSet.builder()
                .subject(subject)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt);
    }
}
