package com.fksoft.domain.booking;

import com.fksoft.domain.auth.UserAccounts;
import com.fksoft.domain.cinema.CinemaCatalog;
import com.fksoft.domain.cinema.SeatType;
import com.fksoft.domain.cinema.SeatView;
import com.fksoft.domain.screening.ScreeningCatalog;
import com.fksoft.domain.screening.ScreeningNotFoundException;
import com.fksoft.domain.screening.ScreeningStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Eligibility and screening-window checks for a reservation (SPEC-0014): verified-email caller, a
 * SCHEDULED screening at least {@code min-lead-minutes} in the future, and the COMPANION-requires-
 * ACCESSIBLE rule. Resolves the room seat map and the hold expiry. Keeps these out of {@link
 * ReservationService} so its constructor stays small.
 */
@Component
@RequiredArgsConstructor
class ReservationPolicy {

    private final UserAccounts userAccounts;
    private final ScreeningCatalog screenings;
    private final CinemaCatalog cinema;
    private final BookingProperties properties;

    ReservationContext validateAndResolve(
            UUID callerId, UUID screeningId, Collection<UUID> selectedSeatIds, Instant now) {
        var verified = userAccounts
                .find(callerId)
                .map(account -> account.emailVerified())
                .orElse(false);
        if (!verified) {
            throw new EmailNotVerifiedException();
        }

        var screening = screenings.find(screeningId).orElseThrow(ScreeningNotFoundException::new);
        if (screening.status() != ScreeningStatus.SCHEDULED) {
            throw new ScreeningNotFoundException();
        }
        if (now.plus(Duration.ofMinutes(properties.minLeadMinutes())).isAfter(screening.startsAt())) {
            throw new ScreeningTooSoonException();
        }

        var seats =
                cinema.seatsOf(screening.roomId()).stream().collect(Collectors.toMap(SeatView::seatId, view -> view));
        requireCompanionHasAccessible(selectedSeatIds, seats);
        return new ReservationContext(screening, seats, now.plus(Duration.ofMinutes(properties.holdMinutes())));
    }

    private void requireCompanionHasAccessible(Collection<UUID> selectedSeatIds, Map<UUID, SeatView> seats) {
        var selectedTypes = selectedSeatIds.stream()
                .map(seats::get)
                .filter(view -> view != null)
                .map(SeatView::type)
                .toList();
        if (selectedTypes.contains(SeatType.COMPANION) && !selectedTypes.contains(SeatType.ACCESSIBLE)) {
            throw new CompanionRequiresAccessibleException();
        }
    }
}
