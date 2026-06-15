package com.fksoft.domain.pricing;

import com.fksoft.domain.cinema.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PricingSeatType} surcharges (SPEC-0012). Module-internal. */
public interface PricingSeatTypeRepository extends JpaRepository<PricingSeatType, SeatType> {}
