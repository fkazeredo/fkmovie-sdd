package com.fksoft.application.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Self-registration payload (SPEC-0004): name 2–100 no control chars, valid email, strong password. */
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = "^[^\\p{Cntrl}]+$", message = "{user.name-invalid}")
                String name,
        @NotBlank @Email String email,
        @NotBlank
                @Size(min = 8, max = 100)
                @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "{auth.password-policy}")
                String password) {}
