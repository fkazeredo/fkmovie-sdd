package com.fksoft.application.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reset payload (SPEC-0004): same password policy as SPEC-0003. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank
                @Size(min = 8, max = 100)
                @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "{auth.password-policy}")
                String newPassword) {}
