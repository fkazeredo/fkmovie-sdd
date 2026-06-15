/**
 * Pricing module (SPEC-0012): computes the price of a seat for a screening from the base price,
 * the seat type surcharge, the screening's weekday (America/Sao_Paulo) and the ticket type
 * (full / half — Lei 12.933). Modifiers are admin-configurable.
 *
 * <p>Public API: the pure {@code PriceCalculator#quote} facade with {@link
 * com.fksoft.domain.pricing.ScreeningPricingContext}, {@link
 * com.fksoft.domain.pricing.PriceQuote} and {@link com.fksoft.domain.pricing.TicketType}.
 * Other modules call the facade (e.g. the booking seat map and reservations); they never touch
 * the config entities. Computation is pure over an in-memory config snapshot.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Pricing")
package com.fksoft.domain.pricing;
