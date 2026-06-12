package com.fksoft.application.auth.api;

import com.fksoft.application.auth.Role;
import com.fksoft.application.auth.UserManagementService;
import com.fksoft.application.auth.UserStatus;
import com.fksoft.shared.pagination.PageResponse;
import com.fksoft.shared.security.UserContextProvider;
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
        var user = userManagement.invite(request.email(), request.name(), request.role(), actingAdminId());
        return AdminUserResponse.from(user);
    }

    @GetMapping
    PageResponse<AdminUserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = userManagement.list(role, status, PageRequest.of(page, size));
        return PageResponse.from(result, AdminUserResponse::from);
    }

    @GetMapping("/{id}")
    AdminUserResponse get(@PathVariable UUID id) {
        return AdminUserResponse.from(userManagement.get(id));
    }

    @PutMapping("/{id}/role")
    AdminUserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {
        return AdminUserResponse.from(userManagement.changeRole(id, request.role(), actingAdminId()));
    }

    @PostMapping("/{id}/disable")
    AdminUserResponse disable(@PathVariable UUID id) {
        return AdminUserResponse.from(userManagement.disable(id, actingAdminId()));
    }

    @PostMapping("/{id}/enable")
    AdminUserResponse enable(@PathVariable UUID id) {
        return AdminUserResponse.from(userManagement.enable(id, actingAdminId()));
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
