package com.test.system.dto.group.member;

import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;

public record GroupMemberResponse(
        Long id,
        Long userId,
        String email,
        String fullName,
        GroupRole role,
        MembershipStatus status
) {}

