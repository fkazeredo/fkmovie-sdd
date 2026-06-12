package com.fksoft.application.pricing;

/**
 * Computed price for a seat (SPEC-0012), in integer cents. {@code priceCents} is the amount for
 * the requested ticket type; {@code fullCents}/{@code halfCents} are exposed for display (the seat
 * map shows the full price).
 */
public record PriceQuote(int fullCents, int halfCents, int priceCents) {}
