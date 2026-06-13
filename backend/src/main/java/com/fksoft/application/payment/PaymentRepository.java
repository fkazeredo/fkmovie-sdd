package com.fksoft.application.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the {@link Payment} ledger (SPEC-0015). Module-internal. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {}
