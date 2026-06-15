package com.fksoft.application.api;

import com.fksoft.application.api.dto.InviteUserRequest;
import com.fksoft.application.api.dto.UpdateRoleRequest;
import com.fksoft.domain.auth.AdminUserResponse;
import com.fksoft.domain.auth.Role;
import com.fksoft.domain.auth.UserManagementService;
import com.fksoft.domain.auth.UserStatus;
import com.fksoft.infra.security.UserContextProvider;
import com.fksoft.infra.web.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin user management endpoints (SPEC-0005). Access is restricted to ROLE_ADMIN by the
 * security chain ({@code /api/admin/**}); domain rules (self/last-admin) live in the service.
 */
@RestController
@RequestMapping("/api/admin/users")
class AdminUserController {

    private final UserManagementService userManagement;
    private final UserContextProvider userContext;

    AdminUserController(UserManagementService userManagement, UserContextProvider userContext) {
        this.userManagement = userManagement;
        this.userContext = userContext;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AdminUserResponse invite(@Valid @RequestBody InviteUserRequest request) {
        return userManagement.invite(request.email(), request.name(), request.role(), actingAdminId());
    }

    @GetMapping
    PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(userManagement.list(role, status, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    AdminUserResponse get(@PathVariable UUID id) {
        return userManagement.get(id);
    }

    @PutMapping("/{id}/role")
    AdminUserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return userManagement.changeRole(id, request.role(), actingAdminId());
    }

    @PostMapping("/{id}/disable")
    AdminUserResponse disable(@PathVariable UUID id) {
        return userManagement.disable(id, actingAdminId());
    }

    @PostMapping("/{id}/enable")
    AdminUserResponse enable(@PathVariable UUID id) {
        return userManagement.enable(id, actingAdminId());
    }

    @PostMapping("/{id}/resend-invitation")
    @ResponseStatus(HttpStatus.OK)
    void resendInvitation(@PathVariable UUID id) {
        userManagement.resendInvitation(id, actingAdminId());
    }

    private UUID actingAdminId() {
        return userContext.currentUser().userId();
    }
}
