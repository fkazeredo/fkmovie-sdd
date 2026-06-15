package com.fksoft.domain.auth;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin user management (SPEC-0005): invite internal users, list/view, change role,
 * disable/enable, resend invitation, and the public accept-invitation flow. Reuses the auth
 * module's token issuer and login machinery (same {@code users} aggregate).
 */
@Service
public class UserManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserManagementService.class);

    private final UserRepository users;
    private final VerificationTokenService tokens;
    private final UserManagementPolicy policy;
    private final SessionIssuer sessionIssuer;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final MeterRegistry meterRegistry;

    /** Collaborators injected by Spring — constructor injection only (CLAUDE.md). */
    public UserManagementService(
            UserRepository users,
            VerificationTokenService tokens,
            UserManagementPolicy policy,
            SessionIssuer sessionIssuer,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher events,
            MeterRegistry meterRegistry) {
        this.users = users;
        this.tokens = tokens;
        this.policy = policy;
        this.sessionIssuer = sessionIssuer;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Invites an internal user: creates a DISABLED, password-less account and sends an
     * invitation email (via {@link UserInvited}).
     *
     * @throws EmailAlreadyRegisteredException when the email already exists.
     */
    @Transactional
    public AdminUserResponse invite(String email, String name, Role role, UUID actingAdminId) {
        var normalizedEmail = User.normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        var now = Instant.now();
        var user = users.save(User.invited(normalizedEmail, name.trim(), role, actingAdminId, now));
        publishInvitation(user, actingAdminId, now);
        meterRegistry.counter("admin.users.invited").increment();
        log.info("admin action acting={} target={} action=invite role={}", actingAdminId, user.id(), role);
        return AdminUserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> list(Role role, UserStatus status, Pageable pageable) {
        return users.search(role, status, pageable).map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return AdminUserResponse.from(users.findById(id).orElseThrow(UserNotFoundException::new));
    }

    /** Changes a user's single active role (SPEC-0005); takes effect on the next token. */
    @Transactional
    public AdminUserResponse changeRole(UUID id, Role newRole, UUID actingAdminId) {
        var user = users.findById(id).orElseThrow(UserNotFoundException::new);
        var oldRole = user.role();
        user.changeRole(newRole);
        events.publishEvent(new UserRoleChanged(user.id(), oldRole, newRole, actingAdminId, Instant.now()));
        meterRegistry.counter("admin.users.role_changed").increment();
        log.info(
                "admin action acting={} target={} action=change-role from={} to={}",
                actingAdminId,
                id,
                oldRole,
                newRole);
        return AdminUserResponse.from(user);
    }

    /** Disables a user after the self / last-admin guards (SPEC-0005). */
    @Transactional
    public AdminUserResponse disable(UUID id, UUID actingAdminId) {
        var user = users.findById(id).orElseThrow(UserNotFoundException::new);
        policy.requireCanDisable(actingAdminId, user, users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE));
        user.disable();
        events.publishEvent(new UserDisabled(user.id(), actingAdminId, Instant.now()));
        meterRegistry.counter("admin.users.disabled").increment();
        log.info("admin action acting={} target={} action=disable", actingAdminId, id);
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse enable(UUID id, UUID actingAdminId) {
        var user = users.findById(id).orElseThrow(UserNotFoundException::new);
        user.enable();
        events.publishEvent(new UserEnabled(user.id(), actingAdminId, Instant.now()));
        log.info("admin action acting={} target={} action=enable", actingAdminId, id);
        return AdminUserResponse.from(user);
    }

    /** Re-sends the invitation while the user is still awaiting acceptance (DISABLED). */
    @Transactional
    public void resendInvitation(UUID id, UUID actingAdminId) {
        var user = users.findById(id).orElseThrow(UserNotFoundException::new);
        if (user.status() != UserStatus.DISABLED) {
            return;
        }
        var now = Instant.now();
        tokens.assertInvitationResendAllowed(user.id(), now);
        publishInvitation(user, actingAdminId, now);
        log.info("admin action acting={} target={} action=resend-invitation", actingAdminId, id);
    }

    /**
     * Accepts an invitation (public): sets the password and name, activates the account and
     * auto-logs the user in.
     *
     * @throws TokenInvalidException unknown token; {@link TokenExpiredException} expired/consumed.
     */
    @Transactional
    public AuthService.AuthResult acceptInvitation(
            String rawToken, String password, String name, String ip, String userAgent) {
        var now = Instant.now();
        var userId = tokens.consumeInvitation(rawToken, now);
        var user = users.findById(userId).orElseThrow(TokenInvalidException::new);
        user.acceptInvitation(passwordEncoder.encode(password), name, now);
        events.publishEvent(new UserInvitationAccepted(user.id(), now));
        log.info("invitation accepted userId={}", user.id());
        return sessionIssuer.issueSession(user, ip, userAgent, now);
    }

    private void publishInvitation(User user, UUID actingAdminId, Instant now) {
        var invitationToken = tokens.issueInvitation(user.id(), now);
        events.publishEvent(new UserInvited(
                user.id(),
                user.email(),
                user.name(),
                user.preferredLocale(),
                user.role(),
                invitationToken,
                actingAdminId,
                now));
    }
}
