package com.fksoft.application.api;

import com.fksoft.application.api.dto.AuthTokensResponse;
import com.fksoft.application.api.dto.ChangePasswordRequest;
import com.fksoft.application.api.dto.LoginRequest;
import com.fksoft.domain.auth.AuthService;
import com.fksoft.domain.auth.InvalidRefreshTokenException;
import com.fksoft.domain.auth.UserSummaryResponse;
import com.fksoft.infra.security.UserContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints (SPEC-0003). The refresh token travels only in an
 * httpOnly+Secure+SameSite=Strict cookie scoped to {@code /api/auth}; the access token is
 * returned in the body and never set as a cookie.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final AuthService authService;
    private final UserContextProvider userContext;
    private final Duration refreshTtl;

    AuthController(
            AuthService authService,
            UserContextProvider userContext,
            @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.authService = authService;
        this.userContext = userContext;
        this.refreshTtl = refreshTtl;
    }

    @PostMapping("/login")
    ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        var result = authService.login(request.email(), request.password(), clientIp(http), userAgent(http));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), refreshTtl).toString())
                .body(AuthTokensResponse.from(result));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthTokensResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken, HttpServletRequest http) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        var result = authService.refresh(refreshToken, clientIp(http), userAgent(http));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), refreshTtl).toString())
                .body(AuthTokensResponse.from(result));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
                .build();
    }

    @PostMapping("/change-password")
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(
                userContext.currentUser().userId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    UserSummaryResponse me() {
        return authService.currentUser(userContext.currentUser().userId());
    }

    private static ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest request) {
        var userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return userAgent == null ? "" : userAgent;
    }
}
