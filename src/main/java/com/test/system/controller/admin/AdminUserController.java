package com.test.system.controller.admin;

import com.test.system.dto.admin.UpdateUserRolesRequest;
import com.test.system.dto.admin.UserListResponse;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.test.system.utils.auth.AuthWebUtils.currentEmail;

/**
 * Admin controller for user management.
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Controller", description = "Admin endpoints for user management (ROLE_ADMIN required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(
            summary = "List all users",
            description = "Returns a list of all users in the system. Only accessible by administrators (ROLE_ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user list",
                    content = @Content(
                            array = @ArraySchema(schema = @Schema(implementation = UserListResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - user not authenticated"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user does not have ROLE_ADMIN"
            )
    })
    @GetMapping
    public List<UserListResponse> listAllUsers(Authentication authentication) {
        String email = requireEmail(authentication);
        return adminUserService.listAllUsers(email);
    }

    @Operation(
            summary = "Enable user account",
            description = "Enables a disabled user account. Only accessible by administrators (ROLE_ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User enabled successfully"),
            @ApiResponse(responseCode = "400", description = "User is already enabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableUser(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        adminUserService.enableUser(id, email);
    }

    @Operation(
            summary = "Disable user account",
            description = "Disables a user account. Cannot disable admin users or yourself. Only accessible by administrators (ROLE_ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User disabled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot disable admin or self, or user already disabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableUser(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        adminUserService.disableUser(id, email);
    }

    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user and all associated data (groups, memberships, tokens, etc.). " +
                    "Cannot delete admin users or yourself. Only accessible by administrators (ROLE_ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete admin or self"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        adminUserService.deleteUser(id, email);
    }

    @Operation(
            summary = "Update user roles",
            description = "Updates the roles assigned to a user. Cannot modify admin users or yourself. " +
                    "ROLE_USER is always required. Only accessible by administrators (ROLE_ADMIN)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Roles updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid roles, cannot modify admin/self, or ROLE_USER missing"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not an admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Authentication authentication
    ) {
        String email = requireEmail(authentication);
        adminUserService.updateUserRoles(id, request.roles(), email);
    }

    /**
     * Extracts and validates email from authentication.
     *
     * @param auth authentication object
     * @return user email
     * @throws UnauthorizedException if not authenticated
     */
    private String requireEmail(Authentication auth) {
        String email = currentEmail(auth);
        if (email == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return email;
    }
}

