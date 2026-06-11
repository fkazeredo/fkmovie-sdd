package com.fksoft.application.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Audit trail for authentication facts (SPEC-0003 observability). AFTER_COMMIT so audit
 * lines never describe rolled-back work.
 */
@Component
class AuthAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditLogger.class);

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
}
