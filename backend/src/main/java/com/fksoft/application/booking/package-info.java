/**
 * Booking module (SPEC-0009, ADR 0004, ADR 0009): per-screening seat inventory and (from later
 * specs) reservations and realtime. v1 materializes one {@code ScreeningSeat} per physical seat
 * of a screening's room, all {@code FREE}, by consuming the {@code ScreeningCreated} event after
 * commit.
 *
 * <p>The seat-map read endpoint (SPEC-0011), reservations (SPEC-0014) and realtime publishing
 * (SPEC-0013) build on this inventory. The module reads room seats through the cinema module's
 * public {@code CinemaCatalog} facade, never its entities.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Booking")
package com.fksoft.application.booking;
