package com.fksoft.infra.web;

import com.fksoft.domain.auth.CannotDisableLastAdminException;
import com.fksoft.domain.auth.CannotDisableSelfException;
import com.fksoft.domain.auth.EmailAlreadyRegisteredException;
import com.fksoft.domain.auth.InvalidCredentialsException;
import com.fksoft.domain.auth.InvalidRefreshTokenException;
import com.fksoft.domain.auth.LoginRateLimitedException;
import com.fksoft.domain.auth.PasswordMismatchException;
import com.fksoft.domain.auth.RegistrationRateLimitedException;
import com.fksoft.domain.auth.TokenExpiredException;
import com.fksoft.domain.auth.TokenInvalidException;
import com.fksoft.domain.auth.TokenReuseDetectedException;
import com.fksoft.domain.auth.UserDisabledException;
import com.fksoft.domain.auth.UserNotFoundException;
import com.fksoft.domain.booking.ActiveReservationExistsException;
import com.fksoft.domain.booking.AlreadyCancelledException;
import com.fksoft.domain.booking.AlreadyConfirmedException;
import com.fksoft.domain.booking.CancellationWindowClosedException;
import com.fksoft.domain.booking.CompanionRequiresAccessibleException;
import com.fksoft.domain.booking.EmailNotVerifiedException;
import com.fksoft.domain.booking.OperatorInvalidSearchException;
import com.fksoft.domain.booking.ReservationAccessDeniedException;
import com.fksoft.domain.booking.ReservationCancelledException;
import com.fksoft.domain.booking.ReservationExpiredException;
import com.fksoft.domain.booking.ReservationNotFoundException;
import com.fksoft.domain.booking.ScreeningTooSoonException;
import com.fksoft.domain.booking.SeatsUnavailableException;
import com.fksoft.domain.booking.TicketNotFoundException;
import com.fksoft.domain.booking.TicketNotReprintableException;
import com.fksoft.domain.error.DomainException;
import com.fksoft.domain.payment.InvalidWebhookPayloadException;
import com.fksoft.domain.payment.InvalidWebhookSignatureException;
import com.fksoft.domain.pricing.PricingInvalidMultiplierException;
import com.fksoft.domain.pricing.PricingInvalidSurchargeException;
import com.fksoft.domain.screening.MovieHasScreeningsException;
import com.fksoft.domain.screening.MovieNotFoundException;
import com.fksoft.domain.screening.ScreeningCancelledException;
import com.fksoft.domain.screening.ScreeningHasReservationsException;
import com.fksoft.domain.screening.ScreeningHasSoldTicketsException;
import com.fksoft.domain.screening.ScreeningInvalidFilterException;
import com.fksoft.domain.screening.ScreeningMovieNotFoundException;
import com.fksoft.domain.screening.ScreeningNotFoundException;
import com.fksoft.domain.screening.ScreeningRoomNotFoundException;
import com.fksoft.domain.screening.ScreeningRoomOverlapException;
import com.fksoft.domain.screening.ScreeningStartsTooSoonException;
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
