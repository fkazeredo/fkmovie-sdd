package com.fksoft.application.api.dto;

import com.fksoft.domain.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin invitation payload (SPEC-0005). */
public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 2, max = 100) @Pattern(regexp = "^[^\\p{Cntrl}]+$", message = "{user.name-invalid}")
                String name,
        @NotNull Role role) {}
