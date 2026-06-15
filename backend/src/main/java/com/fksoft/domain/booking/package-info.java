/**
 * Booking module (SPEC-0009, ADR 0004): per-screening seat inventory and reservations. v1
 * materializes one {@code ScreeningSeat} per physical seat of a screening's room, all
 * {@code FREE}, by consuming the {@code ScreeningCreated} event after commit.
 *
 * <p>The seat-map read endpoint (SPEC-0011) and reservations (SPEC-0014) build on this inventory.
 * Realtime publishing (SPEC-0013, ADR 0012) listens to this module's events from the delivery
 * layer ({@code com.fksoft.application.realtime}). The module reads room seats through the cinema
 * module's public {@code CinemaCatalog} facade, never its entities.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Booking")
package com.fksoft.domain.booking;
