package com.fksoft.domain.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** SPEC-0005 disable guards: not self, not the last active admin. */
class UserManagementPolicyTest {

    private final UserManagementPolicy policy = new UserManagementPolicy();

    @Test
    void cannotDisableSelf() {
        var admin = User.invited("a@x.com", "A", Role.ADMIN, null, java.time.Instant.now());
        assertThatThrownBy(() -> policy.requireCanDisable(admin.id(), admin, 5))
                .isInstanceOf(CannotDisableSelfException.class);
    }

    @Test
    void cannotDisableTheLastActiveAdmin() {
        var target = User.newCustomer("a@x.com", "h", "A", "pt-BR");
        target.changeRole(Role.ADMIN); // ACTIVE admin
        assertThatThrownBy(() -> policy.requireCanDisable(UUID.randomUUID(), target, 1))
                .isInstanceOf(CannotDisableLastAdminException.class);
    }

    @Test
    void canDisableAnAdminWhenOthersRemain() {
        var target = User.newCustomer("a@x.com", "h", "A", "pt-BR");
        target.changeRole(Role.ADMIN);
        assertThatCode(() -> policy.requireCanDisable(UUID.randomUUID(), target, 3))
                .doesNotThrowAnyException();
    }

    @Test
    void canDisableANonAdminRegardlessOfAdminCount() {
        var operator = User.invited("op@x.com", "Op", Role.OPERATOR, null, java.time.Instant.now());
        assertThatCode(() -> policy.requireCanDisable(UUID.randomUUID(), operator, 1))
                .doesNotThrowAnyException();
    }
}
