package com.test.system.dto.group.info;

import com.test.system.dto.group.member.GroupMemberResponse;

import java.util.List;

public record GroupDetailsResponse(
        Long id,
        String name,
        boolean personal,
        Long ownerId,
        String ownerEmail,
        List<GroupMemberResponse> members
) {}

