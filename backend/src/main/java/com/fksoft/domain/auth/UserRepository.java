package com.fksoft.domain.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Lookup by normalized email; the citext column makes the match case-insensitive anyway. */
    Optional<User> findByEmail(String email);

    /** Counts active admins — backs the "cannot disable the last admin" rule (SPEC-0005). */
    long countByRoleAndStatus(Role role, UserStatus status);

    /** Whether any user holds the role — backs the first-admin bootstrap (SPEC-0005). */
    boolean existsByRole(Role role);

    /** Admin listing with optional role/status filters (SPEC-0005). */
    @Query("select u from User u where (:role is null or u.role = :role) "
            + "and (:status is null or u.status = :status)")
    Page<User> search(@Param("role") Role role, @Param("status") UserStatus status, Pageable pageable);
}
