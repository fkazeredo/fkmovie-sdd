package com.fksoft.application.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Lookup by normalized email; the citext column makes the match case-insensitive anyway. */
    Optional<User> findByEmail(String email);
}
