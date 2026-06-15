package com.fksoft.domain.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the {@link Payment} ledger (SPEC-0015). Module-internal. */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** The most recent ledger entry of a kind for a reservation — backs the refund read (SPEC-0019). */
    Optional<Payment> findFirstByReservationIdAndKindOrderByCreatedAtDesc(UUID reservationId, PaymentKind kind);
}
