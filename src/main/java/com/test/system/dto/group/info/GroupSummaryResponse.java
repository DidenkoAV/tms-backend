package com.test.system.dto.group.info;

import com.test.system.enums.groups.GroupType;

public record GroupSummaryResponse(
        Long id,
        String name,
        GroupType groupType,
        Long ownerId,
        String ownerEmail,
        int membersCount
) {}

