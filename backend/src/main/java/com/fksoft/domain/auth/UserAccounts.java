package com.fksoft.domain.auth;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public read API of the auth module (SPEC-0014): the synchronous collaboration point other modules
 * use to read a user account (role, status, email verification). Returns a stable {@link
 * AccountView}, never the {@code User} entity (ArchUnit-enforced).
 */
@Service
@RequiredArgsConstructor
public class UserAccounts {

    private final UserRepository users;

    /** Reads an account as a stable projection (empty if unknown). */
    @Transactional(readOnly = true)
    public Optional<AccountView> find(UUID userId) {
        return users.findById(userId).map(UserAccounts::toView);
    }

    /** Reads an account by email — case-insensitive (citext); backs operator lookup (SPEC-0020). */
    @Transactional(readOnly = true)
    public Optional<AccountView> findByEmail(String email) {
        return users.findByEmail(email).map(UserAccounts::toView);
    }

    private static AccountView toView(User user) {
        return new AccountView(
                user.id(),
                user.email(),
                user.name(),
                user.preferredLocale(),
                user.role(),
                user.status(),
                user.isEmailVerified());
    }
}
