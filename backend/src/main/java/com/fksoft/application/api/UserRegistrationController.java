package com.fksoft.application.api;

import com.fksoft.application.api.dto.AcceptInvitationRequest;
import com.fksoft.application.api.dto.AuthTokensResponse;
import com.fksoft.application.api.dto.ForgotPasswordRequest;
import com.fksoft.application.api.dto.RegisterRequest;
import com.fksoft.application.api.dto.ResendVerificationRequest;
import com.fksoft.application.api.dto.ResetPasswordRequest;
import com.fksoft.application.api.dto.VerifyEmailRequest;
import com.fksoft.domain.auth.CustomerRegistrationService;
import com.fksoft.domain.auth.UserManagementService;
import com.fksoft.domain.auth.VerifyEmailResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer self-service endpoints (SPEC-0004). All are public. Register auto-logs the new
 * customer in, returning the same token pair + refresh cookie as login; the other endpoints
 * are deliberately silent (anti-enumeration) and return no business detail.
 */
@RestController
@RequestMapping("/api/users")
class UserRegistrationController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final List<Locale.LanguageRange> SUPPORTED =
            List.of(new Locale.LanguageRange("pt-BR"), new Locale.LanguageRange("en"));

    private final CustomerRegistrationService registrationService;
    private final UserManagementService userManagementService;
    private final Duration refreshTtl;

    UserRegistrationController(
            CustomerRegistrationService registrationService,
            UserManagementService userManagementService,
            @Value("${app.jwt.refresh-ttl}") Duration refreshTtl) {
        this.registrationService = registrationService;
        this.userManagementService = userManagementService;
        this.refreshTtl = refreshTtl;
    }

    @PostMapping("/accept-invitation")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<AuthTokensResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request, HttpServletRequest http) {
        var result = userManagementService.acceptInvitation(
                request.token(), request.password(), request.name(), http.getRemoteAddr(), userAgent(http));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), refreshTtl).toString())
                .body(AuthTokensResponse.from(result));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<AuthTokensResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(name = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage,
            HttpServletRequest http) {
        var result = registrationService.register(
                request.name(),
                request.email(),
                request.password(),
                resolveLocale(acceptLanguage),
                http.getRemoteAddr(),
                userAgent(http));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken(), refreshTtl).toString())
                .body(AuthTokensResponse.from(result));
    }

    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.OK)
    void resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerification(request.email());
    }

    @PostMapping("/verify-email")
    VerifyEmailResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return registrationService.verifyEmail(request.token());
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        registrationService.forgotPassword(request.email(), http.getRemoteAddr());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        registrationService.resetPassword(request.token(), request.newPassword());
    }

    /** Maps Accept-Language to a supported locale tag; defaults to pt-BR. */
    private static String resolveLocale(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return "pt-BR";
        }
        var match = Locale.lookupTag(Locale.LanguageRange.parse(acceptLanguage), supportedTags());
        return match == null ? "pt-BR" : match;
    }

    private static List<String> supportedTags() {
        return SUPPORTED.stream().map(Locale.LanguageRange::getRange).toList();
    }

    private static String userAgent(HttpServletRequest request) {
        var userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return userAgent == null ? "" : userAgent;
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
}
