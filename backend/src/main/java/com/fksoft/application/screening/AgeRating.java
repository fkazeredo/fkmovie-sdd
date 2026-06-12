package com.fksoft.application.screening;

/**
 * Brazilian content rating (Classificação Indicativa), SPEC-0008. {@code L} = livre (all ages);
 * the {@code A<n>} values are the minimum recommended age.
 */
public enum AgeRating {
    L,
    A10,
    A12,
    A14,
    A16,
    A18
}
