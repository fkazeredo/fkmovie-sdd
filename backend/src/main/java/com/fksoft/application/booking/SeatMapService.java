package com.fksoft.application.booking;

import com.fksoft.application.cinema.CinemaCatalog;
import com.fksoft.application.cinema.RoomView;
import com.fksoft.application.cinema.SeatView;
import com.fksoft.application.pricing.PriceCalculator;
import com.fksoft.application.pricing.ScreeningPricingContext;
import com.fksoft.application.pricing.TicketType;
import com.fksoft.application.screening.ScreeningCancelledException;
import com.fksoft.application.screening.ScreeningCatalog;
import com.fksoft.application.screening.ScreeningNotFoundException;
import com.fksoft.application.screening.ScreeningStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the public seat map of a screening (SPEC-0011): joins the screening (for room, start,
 * base price), the cinema seats (row/number/type) and the screening's seat inventory (status),
 * then prices each seat. Read-only; the realtime channel (SPEC-0013) patches this view.
 */
@Service
public class SeatMapService {

    private final ScreeningCatalog screenings;
    private final CinemaCatalog cinema;
    private final ScreeningSeatRepository screeningSeats;
    private final PriceCalculator pricing;
    private final MeterRegistry meterRegistry;

    SeatMapService(
            ScreeningCatalog screenings,
            CinemaCatalog cinema,
            ScreeningSeatRepository screeningSeats,
            PriceCalculator pricing,
            MeterRegistry meterRegistry) {
        this.screenings = screenings;
        this.cinema = cinema;
        this.screeningSeats = screeningSeats;
        this.pricing = pricing;
        this.meterRegistry = meterRegistry;
    }

    /**
     * @throws ScreeningNotFoundException unknown screening (404);
     *     {@link ScreeningCancelledException} cancelled screening (410).
     */
    @Transactional(readOnly = true)
    public SeatMapResponse seatMap(UUID screeningId) {
        var sample = Timer.start(meterRegistry);
        var outcome = "success";
        try {
            return build(screeningId);
        } catch (ScreeningNotFoundException ex) {
            outcome = "not-found";
            throw ex;
        } catch (ScreeningCancelledException ex) {
            outcome = "cancelled";
            throw ex;
        } finally {
            sample.stop(Timer.builder("seat_map.requests")
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    private SeatMapResponse build(UUID screeningId) {
        var screening = screenings.find(screeningId).orElseThrow(ScreeningNotFoundException::new);
        if (screening.status() == ScreeningStatus.CANCELLED) {
            throw new ScreeningCancelledException();
        }
        var roomName = cinema.findRoom(screening.roomId())
                .map(RoomView::name)
                .orElseThrow(() -> new IllegalStateException("Room missing for screening " + screeningId));
        var seatsBySeatId = cinema.seatsOf(screening.roomId()).stream()
                .collect(Collectors.toMap(SeatView::seatId, Function.identity()));
        var pricingContext = new ScreeningPricingContext(screening.basePriceCents(), screening.startsAt());
        var seats = screeningSeats.findByScreeningId(screeningId).stream()
                .map(inventory -> toSeat(inventory, seatsBySeatId.get(inventory.seatId()), pricingContext))
                .sorted(Comparator.comparing(SeatMapSeat::row).thenComparingInt(SeatMapSeat::number))
                .toList();
        return new SeatMapResponse(screeningId, roomName, screening.startsAt(), seats);
    }

    private SeatMapSeat toSeat(ScreeningSeat inventory, SeatView seat, ScreeningPricingContext pricingContext) {
        return new SeatMapSeat(
                seat.seatId(),
                seat.row(),
                seat.number(),
                seat.type(),
                inventory.status(),
                pricing.quote(pricingContext, seat.type(), TicketType.FULL).fullCents());
    }
}
