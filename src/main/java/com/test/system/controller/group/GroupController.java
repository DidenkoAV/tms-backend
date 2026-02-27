package com.test.system.controller.group;

import com.test.system.dto.group.info.GroupDetailsResponse;
import com.test.system.dto.group.info.GroupSummaryResponse;
import com.test.system.dto.group.invitation.InviteMemberRequest;
import com.test.system.dto.group.management.CreateGroupRequest;
import com.test.system.dto.group.management.RenameGroupRequest;
import com.test.system.dto.group.member.ChangeMemberRoleRequest;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.exceptions.security.RateLimitExceededException;
import com.test.system.service.authorization.user.UserProfileService;
import com.test.system.service.group.GroupInvitationService;
import com.test.system.service.group.GroupManagementService;
import com.test.system.service.group.GroupMemberService;
import com.test.system.service.security.RateLimitService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Group Controller", description = "Manage groups, members, and invitations")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {

    private final GroupManagementService groupManagementService;
    private final GroupInvitationService invitationService;
    private final GroupMemberService memberService;
    private final RateLimitService rateLimitService;
    private final UserProfileService userProfileService;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /* ===================== Queries ===================== */

    /**
     * Get all groups where current user is a member.
     * Returns list of groups with basic information (id, name, role, member count).
     */
    @GetMapping("/my")
    public List<GroupSummaryResponse> getMyGroups(@AuthenticationPrincipal UserDetails principal) {
        return groupManagementService.myGroups(requireEmail(principal));
    }

    /**
     * Create a new group.
     * The current user will be set as the owner of the group.
     * A non-personal group is created.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDetailsResponse createGroup(@Valid @RequestBody CreateGroupRequest request,
                                             @AuthenticationPrincipal UserDetails principal) {
        return groupManagementService.createGroup(request.name(), requireEmail(principal));
    }

    /**
     * Get detailed information about a specific group.
     * Returns group details including name, description, member count, and user's role.
     */
    @GetMapping("/{groupId}")
    public GroupDetailsResponse getGroupById(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails principal) {
        return groupManagementService.getGroupDetails(groupId, requireEmail(principal));
    }

    /**
     * Get list of all members in a group.
     * Returns list of users with their roles (OWNER, ADMIN, MEMBER).
     */
    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> getGroupMembers(@PathVariable Long groupId,
                                                      @AuthenticationPrincipal UserDetails principal) {
        return memberService.getGroupMembers(groupId, requireEmail(principal));
    }

    /* ===================== Group management ===================== */

    /**
     * Rename a group.
     * Only OWNER or ADMIN can rename the group.
     */
    @PatchMapping("/{groupId}")
    public GroupDetailsResponse renameGroup(@PathVariable Long groupId,
                                       @RequestBody RenameGroupRequest rq,
                                       @AuthenticationPrincipal UserDetails principal) {
        String email = requireEmail(principal);
        groupManagementService.updateGroupName(groupId, rq.name(), email);
        return groupManagementService.getGroupDetails(groupId, email);
    }

    /**
     * Delete a group permanently.
     * Only OWNER can delete the group.
     */
    @DeleteMapping("/{groupId}")
    public void deleteGroup(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails principal) {
        groupManagementService.deleteGroup(groupId, requireEmail(principal));
    }

    /**
     * Leave a group.
     * Current user will be removed from the group.
     * OWNER cannot leave - must transfer ownership or delete group first.
     */
    @PostMapping("/{groupId}/leave")
    public void leaveGroup(@PathVariable Long groupId, @AuthenticationPrincipal UserDetails principal) {
        groupManagementService.leaveGroup(groupId, requireEmail(principal));
    }

    /* ===================== Invitations ===================== */

    /**
     * Invite a user to the group by email.
     * Only OWNER or ADMIN can invite new members.
     * An invitation email will be sent to the user.
     */
    @PostMapping("/{groupId}/invites")
    public void inviteUserToGroup(@PathVariable Long groupId,
                                   @RequestBody InviteMemberRequest rq,
                                   @AuthenticationPrincipal UserDetails principal) {
        String email = requireEmail(principal);
        Long userId = userProfileService.getUserByEmail(email).getId();

        // Rate limiting: 10 invites per hour per user
        if (!rateLimitService.allowInvite(userId)) {
            throw new RateLimitExceededException("Too many invitations sent. Please try again later.");
        }

        invitationService.inviteUserToGroup(groupId, email, rq.email());
    }

    /**
     * Get list of pending invitations for a group.
     * Returns users who were invited but haven't accepted yet.
     * Only OWNER or ADMIN can view pending invitations.
     */
    @GetMapping("/{groupId}/invites/pending")
    public List<GroupMemberResponse> getPendingInvitations(@PathVariable Long groupId,
                                                            @AuthenticationPrincipal UserDetails principal) {
        return invitationService.getPendingInvitationsAsDto(groupId, requireEmail(principal));
    }

    /**
     * Cancel a pending invitation.
     * Only OWNER or ADMIN can cancel invitations.
     */
    @DeleteMapping("/{groupId}/invites/{memberId}")
    public void cancelInvitation(@PathVariable Long groupId,
                                  @PathVariable Long memberId,
                                  @AuthenticationPrincipal UserDetails principal) {
        invitationService.cancelPendingInvitation(memberId, requireEmail(principal));
    }

    /**
     * Accept a group invitation from email link.
     *
     * This endpoint is PUBLIC (no authentication required) because:
     * - The invitation token itself contains the user information
     * - User might have expired JWT when clicking email link
     * - Token validation ensures only the invited user can accept
     *
     * Flow for NEW users (placeholder):
     * 1. User clicks email link: http://localhost:8083/api/groups/invites/accept?token=xxx
     * 2. Backend validates token, accepts invitation, enables user
     * 3. Redirects to: http://localhost:5173/invite/accepted?email=user@example.com&groupName=Team&needsPassword=true
     * 4. Frontend shows: "Invitation accepted! Please set your password"
     * 5. User sets password and can access the group
     *
     * Flow for EXISTING users:
     * 1. User clicks email link
     * 2. Backend accepts invitation
     * 3. Redirects to: http://localhost:5173/invite/accepted?email=user@example.com&groupName=Team
     * 4. Frontend shows: "Invitation accepted! Redirecting to groups..."
     */
    @GetMapping("/invites/accept")
    public RedirectView acceptInvitation(@RequestParam String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation token is required");
        }

        // Accept invitation (no authentication required - token contains user info)
        var result = invitationService.acceptGroupInvitationPublic(token, null);

        // Build redirect URL with all necessary information
        StringBuilder redirectUrl = new StringBuilder(frontendBaseUrl + "/invite/accepted");
        redirectUrl.append("?email=").append(java.net.URLEncoder.encode(result.getEmail(), java.nio.charset.StandardCharsets.UTF_8));
        redirectUrl.append("&groupName=").append(java.net.URLEncoder.encode(result.getGroupName(), java.nio.charset.StandardCharsets.UTF_8));

        if (result.isNeedsPassword()) {
            redirectUrl.append("&needsPassword=true");
        }

        return new RedirectView(redirectUrl.toString());
    }

    /* ===================== Members ===================== */

    /**
     * Remove a member from the group.
     * Only OWNER or ADMIN can remove members.
     * OWNER cannot be removed - must transfer ownership first.
     */
    @DeleteMapping("/{groupId}/members/{memberId}")
    public void removeMemberFromGroup(@PathVariable Long groupId,
                                      @PathVariable Long memberId,
                                      @AuthenticationPrincipal UserDetails principal) {
        memberService.removeMember(groupId, memberId, requireEmail(principal));
    }

    /**
     * Change member's role in the group.
     * Only OWNER can change roles.
     * Available roles: OWNER, ADMIN, MEMBER.
     */
    @PatchMapping("/{groupId}/members/{memberId}/role")
    public void changeMemberRole(@PathVariable Long groupId,
                                  @PathVariable Long memberId,
                                  @RequestBody ChangeMemberRoleRequest rq,
                                  @AuthenticationPrincipal UserDetails principal) {
        memberService.changeMemberRole(groupId, memberId, rq.role(), requireEmail(principal));
    }

    /* ===================== Helpers ===================== */

    private String requireEmail(UserDetails principal) {
        if (principal == null || principal.getUsername() == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return principal.getUsername().trim().toLowerCase();
    }
}
