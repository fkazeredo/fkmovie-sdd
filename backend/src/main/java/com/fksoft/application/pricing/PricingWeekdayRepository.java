package com.fksoft.application.pricing;

import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PricingWeekday} multipliers (SPEC-0012). Module-internal. */
public interface PricingWeekdayRepository extends JpaRepository<PricingWeekday, Integer> {}
