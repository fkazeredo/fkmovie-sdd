package com.fksoft.domain.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Audit trail for authentication facts (SPEC-0003 observability). AFTER_COMMIT so audit
 * lines never describe rolled-back work.
 */
@Component
@Slf4j
class AuthAuditLogger {

    @TransactionalEventListener
    void on(UserLoggedIn event) {
        log.info("audit userLoggedIn userId={} ip={} userAgent={}", event.userId(), event.ip(), event.userAgent());
    }

    @TransactionalEventListener
    void on(UserLoggedOut event) {
        log.info("audit userLoggedOut userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(PasswordChanged event) {
        log.info("audit passwordChanged userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(CustomerRegistered event) {
        log.info("audit customerRegistered userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(EmailVerified event) {
        log.info("audit emailVerified userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(PasswordResetCompleted event) {
        log.info("audit passwordResetCompleted userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(UserInvited event) {
        log.info("audit userInvited target={} role={} by={}", event.userId(), event.role(), event.invitedByAdminId());
    }

    @TransactionalEventListener
    void on(UserInvitationAccepted event) {
        log.info("audit userInvitationAccepted userId={}", event.userId());
    }

    @TransactionalEventListener
    void on(UserRoleChanged event) {
        log.info(
                "audit userRoleChanged target={} from={} to={} by={}",
                event.userId(),
                event.oldRole(),
                event.newRole(),
                event.changedByAdminId());
    }

    @TransactionalEventListener
    void on(UserDisabled event) {
        log.info("audit userDisabled target={} by={}", event.userId(), event.disabledByAdminId());
    }

    @TransactionalEventListener
    void on(UserEnabled event) {
        log.info("audit userEnabled target={} by={}", event.userId(), event.enabledByAdminId());
    }
}
