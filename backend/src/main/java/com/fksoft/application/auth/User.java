package com.fksoft.application.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Account identified by a unique, case-insensitive email (SPEC-0003). The entity protects
 * its invariants: emails are normalized to lowercase at storage, the password is only ever
 * stored as a bcrypt hash, and login flows must call {@link #requireActive()}.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    // Nullable: invited internal users (SPEC-0005) have no password until they accept.
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "preferred_locale", nullable = false)
    private String preferredLocale;

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @Column(name = "invited_at")
    private Instant invitedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    /** Creates an ACTIVE user in the default tenant; the email is normalized to lowercase. */
    public User(String email, String passwordHash, String name, Role role) {
        this(email, passwordHash, name, role, "pt-BR");
    }

    private User(String email, String passwordHash, String name, Role role, String preferredLocale) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.preferredLocale = preferredLocale;
    }

    /** Creates an unverified CUSTOMER (SPEC-0004): ACTIVE, but email_verified_at stays NULL. */
    public static User newCustomer(String email, String passwordHash, String name, String preferredLocale) {
        return new User(email, passwordHash, name, Role.CUSTOMER, preferredLocale);
    }

    /**
     * Creates an invited internal user (SPEC-0005): DISABLED, no password and unverified
     * email until the invitation is accepted. Records who invited them and when.
     */
    public static User invited(String email, String name, Role role, UUID invitedByUserId, Instant invitedAt) {
        var user = new User(email, null, name, role, "pt-BR");
        user.status = UserStatus.DISABLED;
        user.invitedByUserId = invitedByUserId;
        user.invitedAt = invitedAt;
        return user;
    }

    /** Lowercase trim applied at every storage path (SPEC-0003: email unique case-insensitive). */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Guards login flows.
     *
     * @throws UserDisabledException when the account is {@link UserStatus#DISABLED}.
     */
    public void requireActive() {
        if (status == UserStatus.DISABLED) {
            throw new UserDisabledException();
        }
    }

    /** Replaces the stored hash; callers are responsible for bcrypt encoding and revocations. */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /** Marks the email verified (SPEC-0004); idempotent — keeps the first verification time. */
    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public void enable() {
        this.status = UserStatus.ACTIVE;
    }

    /** Changes the user's single active role (SPEC-0005). */
    public void changeRole(Role newRole) {
        this.role = newRole;
    }

    /**
     * Accepts an invitation (SPEC-0005): sets the password and (optionally) the name,
     * activates the account and marks the email verified — clicking the emailed link proves
     * control of the address.
     */
    public void acceptInvitation(String passwordHash, String name, Instant now) {
        this.passwordHash = passwordHash;
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.status = UserStatus.ACTIVE;
        verifyEmail(now);
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String tenantId() {
        return tenantId;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String name() {
        return name;
    }

    public Role role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public String preferredLocale() {
        return preferredLocale;
    }

    public Instant invitedAt() {
        return invitedAt;
    }
}
