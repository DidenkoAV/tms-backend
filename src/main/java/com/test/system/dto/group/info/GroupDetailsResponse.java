package com.test.system.dto.group.info;

import com.test.system.dto.group.member.GroupMemberResponse;
import com.test.system.enums.groups.GroupType;

import java.util.List;

public record GroupDetailsResponse(
        Long id,
        String name,
        GroupType groupType,
        Long ownerId,
        String ownerEmail,
        List<GroupMemberResponse> members
) {}

