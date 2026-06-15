package com.fksoft.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.AccessTokens.IssuedAccessToken;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtAccessTokenIssuerTest {

    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789";
    private static final SecretKeySpec KEY = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtAccessTokenIssuer issuer =
            new JwtAccessTokenIssuer(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), Duration.ofMinutes(15));

    @Test
    void issuesHs256TokenWithSpecClaims() {
        var now = Instant.now();

        IssuedAccessToken issued = issuer.issue("user-123", "CUSTOMER", "default", now);

        var jwt = NimbusJwtDecoder.withSecretKey(KEY)
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(issued.token());
        assertThat(jwt.getSubject()).isEqualTo("user-123");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaimAsString("tenantId")).isEqualTo("default");
        assertThat(jwt.getExpiresAt()).isEqualTo(issued.expiresAt().truncatedTo(ChronoUnit.SECONDS));
        assertThat(issued.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
    }
}
