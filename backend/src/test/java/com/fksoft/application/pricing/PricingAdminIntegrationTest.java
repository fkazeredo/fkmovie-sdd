package com.fksoft.application.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.application.auth.RegistrationIntegrationTestSupport;
import com.fksoft.application.cinema.SeatType;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0012 acceptance: admin reads/updates modifiers; updates apply to the next quote. */
class PricingAdminIntegrationTest extends RegistrationIntegrationTestSupport {

    @Autowired
    private PricingSeatTypeRepository seatTypes;

    @Autowired
    private PricingWeekdayRepository weekdays;

    @Autowired
    private PricingConfig config;

    @Autowired
    private PriceCalculator priceCalculator;

    @AfterEach
    void restoreSeedConfig() {
        seatTypes.save(new PricingSeatType(SeatType.STANDARD, 0));
        seatTypes.save(new PricingSeatType(SeatType.VIP, 1000));
        seatTypes.save(new PricingSeatType(SeatType.ACCESSIBLE, 0));
        seatTypes.save(new PricingSeatType(SeatType.COMPANION, 0));
        for (int day = 1; day <= 7; day++) {
            weekdays.save(new PricingWeekday(day, new BigDecimal("1.00")));
        }
        config.reload();
    }

    @Test
    void listsSeededModifiers() throws Exception {
        var bearer = adminBearer();

        var seatTypeList = get("/api/admin/pricing/seat-types", "Authorization", bearer);
        assertThat(seatTypeList.statusCode()).isEqualTo(200);
        assertThat(seatTypeList.body()).contains("\"seatType\":\"VIP\"").contains("\"surchargeCents\":1000");

        var weekdayList = get("/api/admin/pricing/weekdays", "Authorization", bearer);
        assertThat(weekdayList.statusCode()).isEqualTo(200);
        assertThat(weekdayList.body()).contains("\"dayOfWeek\":1").contains("\"dayOfWeek\":7");
    }

    @Test
    void updatingSeatTypeAppliesToNextQuote() throws Exception {
        var bearer = adminBearer();

        var update = putJson(
                "/api/admin/pricing/seat-types",
                "{\"seatTypes\":[{\"seatType\":\"VIP\",\"surchargeCents\":2000}]}",
                "Authorization",
                bearer);
        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.body()).contains("\"surchargeCents\":2000");

        // Monday, multiplier 1.00 → (3000 + 2000) × 1.00 = 5000.
        var quote = priceCalculator.quote(
                new ScreeningPricingContext(3000, Instant.parse("2026-01-05T18:00:00Z")),
                SeatType.VIP,
                TicketType.FULL);
        assertThat(quote.fullCents()).isEqualTo(5000);
    }

    @Test
    void rejectsPositiveSurchargeOnAccessibleSeats() throws Exception {
        var bearer = adminBearer();

        var response = putJson(
                "/api/admin/pricing/seat-types",
                "{\"seatTypes\":[{\"seatType\":\"ACCESSIBLE\",\"surchargeCents\":500}]}",
                "Authorization",
                bearer);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"pricing.invalid-surcharge\"");
    }

    @Test
    void rejectsMultiplierOutOfRange() throws Exception {
        var bearer = adminBearer();

        var response = putJson(
                "/api/admin/pricing/weekdays",
                "{\"weekdays\":[{\"dayOfWeek\":3,\"multiplier\":3.00}]}",
                "Authorization",
                bearer);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"pricing.invalid-multiplier\"");
    }

    @Test
    void nonAdminIsForbiddenAndAnonymousIsUnauthorized() throws Exception {
        var customerEmail = uniqueEmail();
        register(customerEmail);
        var customerBearer = "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(customerEmail, PASSWORD)));

        assertThat(get("/api/admin/pricing/seat-types", "Authorization", customerBearer)
                        .statusCode())
                .isEqualTo(403);
        assertThat(get("/api/admin/pricing/seat-types").statusCode()).isEqualTo(401);
    }

    private String adminBearer() throws IOException, InterruptedException {
        var email = uniqueEmail();
        seedAdmin(email);
        return bearerFor(email);
    }
}
