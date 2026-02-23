package com.test.system.component.group;

import com.test.system.dto.group.info.GroupDetailsResponse;
import com.test.system.dto.group.info.GroupSummaryResponse;
import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper component for converting Group entities to DTOs.
 * Centralizes all mapping logic for group-related data.
 */
@Component
public class GroupMapper {

    /**
     * Converts GroupMembership to GroupMemberResponse DTO.
     *
     * @param m GroupMembership entity
     * @return GroupMemberResponse DTO
     */
    public GroupMemberResponse toMemberDto(GroupMembership m) {
        return new GroupMemberResponse(
                m.getId(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getUser().getFullName(),
                m.getRole(),
                m.getStatus()
        );
    }

    /**
     * Converts Group to GroupSummaryResponse DTO.
     *
     * @param group Group entity
     * @param membersCount number of active members
     * @return GroupSummaryResponse DTO
     */
    public GroupSummaryResponse toSummaryDto(Group group, int membersCount) {
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.isPersonal(),
                group.getOwner().getId(),
                group.getOwner().getEmail(),
                membersCount
        );
    }

    /**
     * Converts Group to GroupDetailsResponse DTO.
     *
     * @param group Group entity
     * @param members list of group members
     * @return GroupDetailsResponse DTO
     */
    public GroupDetailsResponse toDetailsDto(Group group, List<GroupMemberResponse> members) {
        return new GroupDetailsResponse(
                group.getId(),
                group.getName(),
                group.isPersonal(),
                group.getOwner().getId(),
                group.getOwner().getEmail(),
                members
        );
    }
}

