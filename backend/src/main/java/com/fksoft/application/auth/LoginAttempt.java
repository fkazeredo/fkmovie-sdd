package com.fksoft.application.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Append-only login trail backing the rate limiter (SPEC-0003: 5 failures per email and
 * 20 per IP in 15 minutes). The timestamp is a constructor argument so tests can backdate.
 */
@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private boolean succeeded;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected LoginAttempt() {
        // JPA
    }

    public LoginAttempt(String email, String ip, boolean succeeded, Instant attemptedAt) {
        this.email = email;
        this.ip = ip;
        this.succeeded = succeeded;
        this.attemptedAt = attemptedAt;
    }

    public String email() {
        return email;
    }

    public boolean succeeded() {
        return succeeded;
    }

    public Instant attemptedAt() {
        return attemptedAt;
    }
}
