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

    @Column(name = "password_hash", nullable = false)
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    /** Creates an ACTIVE user in the default tenant; the email is normalized to lowercase. */
    public User(String email, String passwordHash, String name, Role role) {
        this.id = UUID.randomUUID();
        this.tenantId = "default";
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = UserStatus.ACTIVE;
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

    public void disable() {
        this.status = UserStatus.DISABLED;
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
}
