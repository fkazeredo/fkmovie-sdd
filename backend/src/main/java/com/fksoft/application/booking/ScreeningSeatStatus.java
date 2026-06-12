package com.fksoft.application.booking;

/**
 * Availability of a seat for a specific screening (SPEC-0009/0011). Physical seats have no
 * status; availability lives here. {@code HELD}/{@code SOLD} come into use with reservations
 * (SPEC-0014); v1 only creates {@code FREE} rows.
 */
public enum ScreeningSeatStatus {
    FREE,
    HELD,
    SOLD
}
