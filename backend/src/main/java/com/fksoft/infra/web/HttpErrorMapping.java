package com.fksoft.infra.web;

import com.fksoft.application.auth.CannotDisableLastAdminException;
import com.fksoft.application.auth.CannotDisableSelfException;
import com.fksoft.application.auth.EmailAlreadyRegisteredException;
import com.fksoft.application.auth.InvalidCredentialsException;
import com.fksoft.application.auth.InvalidRefreshTokenException;
import com.fksoft.application.auth.LoginRateLimitedException;
import com.fksoft.application.auth.PasswordMismatchException;
import com.fksoft.application.auth.RegistrationRateLimitedException;
import com.fksoft.application.auth.TokenExpiredException;
import com.fksoft.application.auth.TokenInvalidException;
import com.fksoft.application.auth.TokenReuseDetectedException;
import com.fksoft.application.auth.UserDisabledException;
import com.fksoft.application.auth.UserNotFoundException;
import com.fksoft.application.booking.ActiveReservationExistsException;
import com.fksoft.application.booking.AlreadyCancelledException;
import com.fksoft.application.booking.AlreadyConfirmedException;
import com.fksoft.application.booking.CancellationWindowClosedException;
import com.fksoft.application.booking.CompanionRequiresAccessibleException;
import com.fksoft.application.booking.EmailNotVerifiedException;
import com.fksoft.application.booking.OperatorInvalidSearchException;
import com.fksoft.application.booking.ReservationAccessDeniedException;
import com.fksoft.application.booking.ReservationCancelledException;
import com.fksoft.application.booking.ReservationExpiredException;
import com.fksoft.application.booking.ReservationNotFoundException;
import com.fksoft.application.booking.ScreeningTooSoonException;
import com.fksoft.application.booking.SeatsUnavailableException;
import com.fksoft.application.booking.TicketNotFoundException;
import com.fksoft.application.booking.TicketNotReprintableException;
import com.fksoft.application.payment.InvalidWebhookPayloadException;
import com.fksoft.application.payment.InvalidWebhookSignatureException;
import com.fksoft.application.pricing.PricingInvalidMultiplierException;
import com.fksoft.application.pricing.PricingInvalidSurchargeException;
import com.fksoft.application.screening.MovieHasScreeningsException;
import com.fksoft.application.screening.MovieNotFoundException;
import com.fksoft.application.screening.ScreeningCancelledException;
import com.fksoft.application.screening.ScreeningHasReservationsException;
import com.fksoft.application.screening.ScreeningHasSoldTicketsException;
import com.fksoft.application.screening.ScreeningInvalidFilterException;
import com.fksoft.application.screening.ScreeningMovieNotFoundException;
import com.fksoft.application.screening.ScreeningNotFoundException;
import com.fksoft.application.screening.ScreeningRoomNotFoundException;
import com.fksoft.application.screening.ScreeningRoomOverlapException;
import com.fksoft.application.screening.ScreeningStartsTooSoonException;
import com.fksoft.shared.error.DomainException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Presentation-owned mapping from a domain exception type to its HTTP status (ADR 0011). The domain
 * exceptions are transport-agnostic; this registry is the single place where the protocol status is
 * decided. A type with no entry falls back to {@code 422 UNPROCESSABLE_ENTITY} (a business rule
 * rejected the request) — but {@code HttpErrorMappingCompletenessTest} fails the build if any
 * {@link DomainException} subclass is missing here, so the fallback never hides a forgotten mapping.
 */
final class HttpErrorMapping {

    private static final HttpStatus DEFAULT_STATUS = HttpStatus.UNPROCESSABLE_ENTITY;

    private static final Map<Class<? extends DomainException>, HttpStatus> STATUSES = Map.ofEntries(
            // 401 Unauthorized
            Map.entry(InvalidCredentialsException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(InvalidRefreshTokenException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(PasswordMismatchException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(TokenReuseDetectedException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(InvalidWebhookSignatureException.class, HttpStatus.UNAUTHORIZED),
            // 403 Forbidden
            Map.entry(UserDisabledException.class, HttpStatus.FORBIDDEN),
            Map.entry(ReservationAccessDeniedException.class, HttpStatus.FORBIDDEN),
            Map.entry(EmailNotVerifiedException.class, HttpStatus.FORBIDDEN),
            // 404 Not Found
            Map.entry(UserNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(ReservationNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(TicketNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(MovieNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(ScreeningNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(ScreeningMovieNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(ScreeningRoomNotFoundException.class, HttpStatus.NOT_FOUND),
            // 400 Bad Request
            Map.entry(TokenInvalidException.class, HttpStatus.BAD_REQUEST),
            Map.entry(OperatorInvalidSearchException.class, HttpStatus.BAD_REQUEST),
            Map.entry(ScreeningInvalidFilterException.class, HttpStatus.BAD_REQUEST),
            Map.entry(ScreeningStartsTooSoonException.class, HttpStatus.BAD_REQUEST),
            Map.entry(PricingInvalidSurchargeException.class, HttpStatus.BAD_REQUEST),
            Map.entry(PricingInvalidMultiplierException.class, HttpStatus.BAD_REQUEST),
            // 409 Conflict
            Map.entry(EmailAlreadyRegisteredException.class, HttpStatus.CONFLICT),
            Map.entry(CannotDisableSelfException.class, HttpStatus.CONFLICT),
            Map.entry(CannotDisableLastAdminException.class, HttpStatus.CONFLICT),
            Map.entry(AlreadyCancelledException.class, HttpStatus.CONFLICT),
            Map.entry(AlreadyConfirmedException.class, HttpStatus.CONFLICT),
            Map.entry(ReservationCancelledException.class, HttpStatus.CONFLICT),
            Map.entry(CancellationWindowClosedException.class, HttpStatus.CONFLICT),
            Map.entry(TicketNotReprintableException.class, HttpStatus.CONFLICT),
            Map.entry(SeatsUnavailableException.class, HttpStatus.CONFLICT),
            Map.entry(CompanionRequiresAccessibleException.class, HttpStatus.CONFLICT),
            Map.entry(ActiveReservationExistsException.class, HttpStatus.CONFLICT),
            Map.entry(ScreeningRoomOverlapException.class, HttpStatus.CONFLICT),
            Map.entry(ScreeningHasReservationsException.class, HttpStatus.CONFLICT),
            Map.entry(ScreeningHasSoldTicketsException.class, HttpStatus.CONFLICT),
            Map.entry(MovieHasScreeningsException.class, HttpStatus.CONFLICT),
            // 410 Gone
            Map.entry(ReservationExpiredException.class, HttpStatus.GONE),
            Map.entry(TokenExpiredException.class, HttpStatus.GONE),
            Map.entry(ScreeningCancelledException.class, HttpStatus.GONE),
            // 422 Unprocessable Entity
            Map.entry(ScreeningTooSoonException.class, HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(InvalidWebhookPayloadException.class, HttpStatus.UNPROCESSABLE_ENTITY),
            // 429 Too Many Requests
            Map.entry(LoginRateLimitedException.class, HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(RegistrationRateLimitedException.class, HttpStatus.TOO_MANY_REQUESTS));

    private HttpErrorMapping() {}

    static HttpStatus statusOf(Class<? extends DomainException> type) {
        return STATUSES.getOrDefault(type, DEFAULT_STATUS);
    }

    /** The set of explicitly mapped exception types — used by the completeness test. */
    static Set<Class<? extends DomainException>> mappedTypes() {
        return STATUSES.keySet();
    }
}
