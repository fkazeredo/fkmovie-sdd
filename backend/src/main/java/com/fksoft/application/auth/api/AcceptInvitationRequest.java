package com.fksoft.application.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Accept-invitation payload (SPEC-0005): same password policy as SPEC-0003. */
public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank
                @Size(min = 8, max = 100)
                @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "{auth.password-policy}")
                String password,
        @Size(min = 2, max = 100) @Pattern(regexp = "^[^\\p{Cntrl}]*$", message = "{user.name-invalid}") String name) {}
