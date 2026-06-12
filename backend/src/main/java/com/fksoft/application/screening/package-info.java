/**
 * Screening module (SPEC-0008, ADR 0001): the movie catalog and (from SPEC-0009) screenings.
 * v1 ships {@code Movie} with an admin-only CRUD; {@code Screening} and seat-inventory
 * materialization arrive with SPEC-0009.
 *
 * <p>Public API of the module: the {@code MovieArchived} domain event (its consumer — the
 * public-list cache hook — lands with SPEC-0010, so it has no listener yet). The movie
 * deletion guard is a deferred seam: it reports "no screenings" until SPEC-0009 wires the real
 * check — see {@code architecture/simulation-and-mocking.md}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Screening")
package com.fksoft.application.screening;
