package com.fksoft.application.api;

import com.fksoft.application.api.dto.UpdateSeatTypesRequest;
import com.fksoft.application.api.dto.UpdateWeekdaysRequest;
import com.fksoft.domain.cinema.SeatType;
import com.fksoft.domain.pricing.PricingAdminService;
import com.fksoft.domain.pricing.SeatTypeSurchargeResponse;
import com.fksoft.domain.pricing.WeekdayMultiplierResponse;
import com.fksoft.infra.security.UserContextProvider;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin pricing-modifier endpoints (SPEC-0012). Restricted to ROLE_ADMIN by the security chain
 * ({@code /api/admin/**}); validation lives in {@link PricingAdminService}.
 */
@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
class PricingAdminController {

    private final PricingAdminService pricing;
    private final UserContextProvider userContext;

    @GetMapping("/seat-types")
    List<SeatTypeSurchargeResponse> seatTypes() {
        return pricing.listSeatTypes();
    }

    @PutMapping("/seat-types")
    List<SeatTypeSurchargeResponse> updateSeatTypes(@Valid @RequestBody UpdateSeatTypesRequest request) {
        Map<SeatType, Integer> surcharges = request.seatTypes().stream()
                .collect(Collectors.toMap(
                        UpdateSeatTypesRequest.Entry::seatType, UpdateSeatTypesRequest.Entry::surchargeCents));
        return pricing.updateSeatTypes(surcharges, actingAdminId());
    }

    @GetMapping("/weekdays")
    List<WeekdayMultiplierResponse> weekdays() {
        return pricing.listWeekdays();
    }

    @PutMapping("/weekdays")
    List<WeekdayMultiplierResponse> updateWeekdays(@Valid @RequestBody UpdateWeekdaysRequest request) {
        Map<Integer, BigDecimal> multipliers = request.weekdays().stream()
                .collect(Collectors.toMap(
                        UpdateWeekdaysRequest.Entry::dayOfWeek, UpdateWeekdaysRequest.Entry::multiplier));
        return pricing.updateWeekdays(multipliers, actingAdminId());
    }

    private UUID actingAdminId() {
        return userContext.currentUser().userId();
    }
}
