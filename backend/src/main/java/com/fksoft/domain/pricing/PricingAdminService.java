package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin management of pricing modifiers (SPEC-0012): list and update seat-type surcharges and
 * weekday multipliers. Validates the legal/business rules (ACCESSIBLE/COMPANION never surcharged;
 * multiplier in range), audits before/after, and publishes {@link PricingConfigChanged} so the
 * in-memory snapshot reloads after commit. Changes affect only future quotes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PricingAdminService {

    private static final BigDecimal MIN_MULTIPLIER = new BigDecimal("0.10");
    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("2.00");

    private final PricingSeatTypeRepository seatTypes;
    private final PricingWeekdayRepository weekdays;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public List<SeatTypeSurchargeResponse> listSeatTypes() {
        return seatTypes.findAll().stream().map(SeatTypeSurchargeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<WeekdayMultiplierResponse> listWeekdays() {
        return weekdays.findAll().stream().map(WeekdayMultiplierResponse::from).toList();
    }

    /** Updates seat-type surcharges (SPEC-0012); ACCESSIBLE/COMPANION must stay 0. */
    @Transactional
    public List<SeatTypeSurchargeResponse> updateSeatTypes(Map<SeatType, Integer> surcharges, UUID actingAdminId) {
        surcharges.forEach((type, cents) -> {
            requireValidSurcharge(type, cents);
            var entity = seatTypes.findById(type).orElseGet(() -> new PricingSeatType(type, cents));
            log.info(
                    "admin pricing change acting={} seatType={} surcharge from={} to={}",
                    actingAdminId,
                    type,
                    entity.surchargeCents(),
                    cents);
            entity.changeSurcharge(cents);
            seatTypes.save(entity);
        });
        events.publishEvent(new PricingConfigChanged(actingAdminId, Instant.now()));
        return listSeatTypes();
    }

    /** Updates weekday multipliers (SPEC-0012); each must be within [0.10, 2.00]. */
    @Transactional
    public List<WeekdayMultiplierResponse> updateWeekdays(Map<Integer, BigDecimal> multipliers, UUID actingAdminId) {
        multipliers.forEach((day, multiplier) -> {
            requireValidMultiplier(multiplier);
            var entity = weekdays.findById(day).orElseGet(() -> new PricingWeekday(day, multiplier));
            log.info(
                    "admin pricing change acting={} weekday={} multiplier from={} to={}",
                    actingAdminId,
                    day,
                    entity.multiplier(),
                    multiplier);
            entity.changeMultiplier(multiplier);
            weekdays.save(entity);
        });
        events.publishEvent(new PricingConfigChanged(actingAdminId, Instant.now()));
        return listWeekdays();
    }

    private void requireValidSurcharge(SeatType type, int cents) {
        var accessibilitySeat = type == SeatType.ACCESSIBLE || type == SeatType.COMPANION;
        if (cents < 0 || (accessibilitySeat && cents != 0)) {
            throw new PricingInvalidSurchargeException();
        }
    }

    private void requireValidMultiplier(BigDecimal multiplier) {
        if (multiplier.compareTo(MIN_MULTIPLIER) < 0 || multiplier.compareTo(MAX_MULTIPLIER) > 0) {
            throw new PricingInvalidMultiplierException();
        }
    }
}
