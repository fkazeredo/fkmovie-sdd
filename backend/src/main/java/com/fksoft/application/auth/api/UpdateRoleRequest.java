package com.fksoft.application.auth.api;

import com.fksoft.application.auth.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}
