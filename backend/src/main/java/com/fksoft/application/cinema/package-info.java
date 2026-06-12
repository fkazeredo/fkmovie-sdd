/**
 * Cinema module (SPEC-0007, ADR 0001): the physical cinema structure — {@code CinemaRoom}s and
 * typed {@code Seat}s. Rooms are seed-managed in v1 (no REST CRUD) and a physical seat has no
 * availability status (availability is per-screening, SPEC-0011).
 *
 * <p>The module has no public API yet: nothing consumes it. A cross-module seat read-API is a
 * deferred seam introduced by its first consumer (SPEC-0011), and room/seat CRUD is left to a
 * future spec — see {@code architecture/simulation-and-mocking.md}. No facade is built
 * speculatively.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Cinema")
package com.fksoft.application.cinema;
