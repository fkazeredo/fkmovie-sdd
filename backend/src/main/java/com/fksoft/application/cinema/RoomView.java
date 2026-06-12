package com.fksoft.application.cinema;

import java.util.UUID;

/**
 * Stable room projection exposed to other modules (SPEC-0011). Part of the cinema module's
 * public read API ({@link CinemaCatalog}); never the {@code CinemaRoom} entity.
 */
public record RoomView(UUID id, String name) {}
