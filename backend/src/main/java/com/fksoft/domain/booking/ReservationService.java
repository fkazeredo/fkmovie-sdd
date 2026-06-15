package com.fksoft.domain.booking;

import com.fksoft.domain.error.DomainException;
import com.fksoft.domain.pricing.PriceCalculator;
import com.fksoft.domain.pricing.ScreeningPricingContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and reads temporary seat reservations (SPEC-0014, ADR 0004). The critical section holds
 * the target {@code ScreeningSeat} rows with a pessimistic write lock, verifies all are FREE
 * (all-or-nothing), snapshots each seat's price via the pricing module, transitions FREE→HELD and
 * opens a PENDING reservation — making double booking impossible.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private static final List<ReservationStatus> ACTIVE_STATUSES =
            List.of(ReservationStatus.PENDING, ReservationStatus.AWAITING_PAYMENT);

    private final ReservationRepository reservations;
    private final ScreeningSeatRepository screeningSeats;
    private final ReservationPolicy policy;
    private final PriceCalculator pricing;
    private final ReservationViewBuilder viewBuilder;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    ReservationService(
            ReservationRepository reservations,
            ScreeningSeatRepository screeningSeats,
            ReservationPolicy policy,
            PriceCalculator pricing,
            ReservationViewBuilder viewBuilder,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.reservations = reservations;
        this.screeningSeats = screeningSeats;
        this.policy = policy;
        this.pricing = pricing;
        this.viewBuilder = viewBuilder;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Holds the selected seats for the caller (SPEC-0014): all-or-nothing, price snapshotted,
     * concurrency-safe. See the class doc for the locking protocol.
     */
    @Transactional
    public ReservationView reserve(UUID callerId, UUID screeningId, List<SeatSelection> selections) {
        var sample = Timer.start(meterRegistry);
        try {
            return doReserve(callerId, screeningId, selections);
        } catch (DomainException ex) {
            meterRegistry
                    .counter("reservations_rejected_total", "reason", ex.code())
                    .increment();
            throw ex;
        } finally {
            sample.stop(Timer.builder("reservation_create_latency")
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private ReservationView doReserve(UUID callerId, UUID screeningId, List<SeatSelection> selections) {
        var now = Instant.now();
        var seatIds = distinctSeatIds(selections);
        var ctx = policy.validateAndResolve(callerId, screeningId, seatIds, now);
        if (reservations.existsByUserIdAndScreeningIdAndStatusIn(callerId, screeningId, ACTIVE_STATUSES)) {
            throw new ActiveReservationExistsException();
        }

        var lockedBySeatId = screeningSeats.lockForReservation(screeningId, seatIds).stream()
                .collect(Collectors.toMap(ScreeningSeat::seatId, seat -> seat));
        var unavailable = seatIds.stream()
                .filter(id -> lockedBySeatId.get(id) == null
                        || !lockedBySeatId.get(id).isFree())
                .toList();
        if (!unavailable.isEmpty()) {
            throw new SeatsUnavailableException(unavailable);
        }

        var pricingCtx = new ScreeningPricingContext(
                ctx.screening().basePriceCents(), ctx.screening().startsAt());
        var reservation = Reservation.pending(callerId, screeningId, ctx.expiresAt());
        var selectionBySeatId = selections.stream().collect(Collectors.toMap(SeatSelection::seatId, s -> s));
        for (var seatId : seatIds) {
            var selection = selectionBySeatId.get(seatId);
            var seat = lockedBySeatId.get(seatId);
            var price = pricing.quote(pricingCtx, ctx.seats().get(seatId).type(), selection.ticketType())
                    .priceCents();
            reservation.addSeat(new ReservationSeat(
                    seat.id(),
                    selection.ticketType(),
                    selection.halfPriceCategory(),
                    selection.documentReference(),
                    price));
            seat.hold();
        }

        var saved = saveHandlingActiveConflict(reservation);
        events.publishEvent(new ReservationCreated(saved.id(), callerId, screeningId, saved.totalCents(), now));
        events.publishEvent(new SeatsStatusChanged(screeningId, ScreeningSeatStatus.HELD, List.copyOf(seatIds), now));
        meterRegistry.counter("reservations_created_total").increment();
        log.info(
                "reservation created userId={} screeningId={} seats={} reservationId={}",
                callerId,
                screeningId,
                seatIds.size(),
                saved.id());
        return viewBuilder.build(saved);
    }

    /** Reads a reservation (SPEC-0014): owner or OPERATOR/ADMIN only. */
    @Transactional(readOnly = true)
    public ReservationView get(UUID reservationId, UUID callerId, String callerRole) {
        var reservation = reservations.findById(reservationId).orElseThrow(ReservationNotFoundException::new);
        var isStaff = "OPERATOR".equals(callerRole) || "ADMIN".equals(callerRole);
        if (!reservation.userId().equals(callerId) && !isStaff) {
            throw new ReservationAccessDeniedException();
        }
        return viewBuilder.build(reservation);
    }

    private List<UUID> distinctSeatIds(List<SeatSelection> selections) {
        var seen = new LinkedHashSet<UUID>();
        var duplicates = new ArrayList<UUID>();
        for (var selection : selections) {
            if (!seen.add(selection.seatId())) {
                duplicates.add(selection.seatId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new SeatsUnavailableException(duplicates);
        }
        return List.copyOf(seen);
    }

    private Reservation saveHandlingActiveConflict(Reservation reservation) {
        try {
            return reservations.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            // The partial unique index caught a concurrent active reservation for this screening.
            throw new ActiveReservationExistsException();
        }
    }
}
