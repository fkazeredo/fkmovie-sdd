package com.fksoft.domain.auth;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Authorization rules for admin user management that depend on domain state (SPEC-0005),
 * beyond the role check Spring Security already enforces.
 */
@Component
class UserManagementPolicy {

    /**
     * Guards disabling a user.
     *
     * @throws CannotDisableSelfException when an admin targets their own account.
     * @throws CannotDisableLastAdminException when disabling would leave no active admin.
     */
    void requireCanDisable(UUID actingAdminId, User target, long activeAdminCount) {
        if (target.id().equals(actingAdminId)) {
            throw new CannotDisableSelfException();
        }
        if (target.role() == Role.ADMIN && target.status() == UserStatus.ACTIVE && activeAdminCount <= 1) {
            throw new CannotDisableLastAdminException();
        }
    }
}
