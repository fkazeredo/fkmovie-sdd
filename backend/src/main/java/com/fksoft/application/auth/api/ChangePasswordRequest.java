package com.fksoft.application.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** New password policy per SPEC-0003: 8+ chars, at least one letter and one digit. */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank
                @Size(min = 8, max = 100)
                @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "{auth.password-policy}")
                String newPassword) {}
