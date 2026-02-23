package com.test.system.dto.group.info;

public record GroupSummaryResponse(
        Long id,
        String name,
        boolean personal,
        Long ownerId,
        String ownerEmail,
        int membersCount
) {}

