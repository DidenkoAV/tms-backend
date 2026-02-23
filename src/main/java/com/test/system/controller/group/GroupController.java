package com.test.system.controller.group;

import com.test.system.dto.group.info.GroupDetailsResponse;
import com.test.system.dto.group.info.GroupSummaryResponse;
import com.test.system.dto.group.invitation.AcceptInvitationRequest;
import com.test.system.dto.group.invitation.InviteMemberRequest;
import com.test.system.dto.group.management.RenameGroupRequest;
import com.test.system.dto.group.member.ChangeMemberRoleRequest;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.service.group.GroupInvitationService;
import com.test.system.service.group.GroupManagementService;
import com.test.system.service.group.GroupMemberService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    /* ===================== Queries ===================== */

    @GetMapping("/my")
    public List<GroupSummaryResponse> myGroups(@AuthenticationPrincipal UserDetails principal) {
        return groupManagementService.myGroups(requireEmail(principal));
    }

    @GetMapping("/{id}")
    public GroupDetailsResponse group(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return groupManagementService.getGroupDetails(id, requireEmail(principal));
    }

    /* ===================== Group management ===================== */

    @PatchMapping("/{id}")
    public GroupDetailsResponse renameGroup(@PathVariable Long id,
                                       @RequestBody RenameGroupRequest rq,
                                       @AuthenticationPrincipal UserDetails principal) {
        String email = requireEmail(principal);
        groupManagementService.updateGroupName(id, rq.name(), email);
        return groupManagementService.getGroupDetails(id, email);
    }

    @DeleteMapping("/{id}")
    public void deleteGroup(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        groupManagementService.deleteGroup(id, requireEmail(principal));
    }

    @PostMapping("/{id}/leave")
    public void leaveGroup(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        groupManagementService.leaveGroup(id, requireEmail(principal));
    }

    /* ===================== Invitations ===================== */

    @PostMapping("/{id}/members")
    public void invite(@PathVariable Long id,
                       @RequestBody InviteMemberRequest rq,
                       @AuthenticationPrincipal UserDetails principal) {
        invitationService.inviteUserToGroup(id, requireEmail(principal), rq.email());
    }

    @GetMapping("/{id}/invites/pending")
    public List<GroupMemberResponse> pending(@PathVariable Long id,
                                        @AuthenticationPrincipal UserDetails principal) {
        return invitationService.getPendingInvitationsAsDto(id, requireEmail(principal));
    }

    @DeleteMapping("/{id}/invites/{memberId}")
    public void cancelInvite(@PathVariable Long id,
                             @PathVariable Long memberId,
                             @AuthenticationPrincipal UserDetails principal) {
        invitationService.cancelPendingInvitation(memberId, requireEmail(principal));
    }

    @PostMapping("/invite/accept")
    public void accept(@RequestBody AcceptInvitationRequest rq,
                       @AuthenticationPrincipal UserDetails principal) {
        invitationService.acceptGroupInvitation(rq.token(), requireEmail(principal));
    }

    /* ===================== Members ===================== */

    @DeleteMapping("/{id}/members/{memberId}")
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long memberId,
                             @AuthenticationPrincipal UserDetails principal) {
        memberService.removeMember(id, memberId, requireEmail(principal));
    }

    @PatchMapping("/{id}/members/{memberId}")
    public void changeRole(@PathVariable Long id,
                           @PathVariable Long memberId,
                           @RequestBody ChangeMemberRoleRequest rq,
                           @AuthenticationPrincipal UserDetails principal) {
        memberService.changeMemberRole(id, memberId, rq.role(), requireEmail(principal));
    }

    /* ===================== Helpers ===================== */

    private String requireEmail(UserDetails principal) {
        if (principal == null || principal.getUsername() == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return principal.getUsername().trim().toLowerCase();
    }
}
