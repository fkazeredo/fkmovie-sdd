package com.fksoft.application.api.dto;

import com.fksoft.domain.auth.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}
