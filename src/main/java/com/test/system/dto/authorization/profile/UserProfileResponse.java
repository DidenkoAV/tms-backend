package com.test.system.dto.authorization.profile;

import com.test.system.dto.group.info.GroupInfoResponse;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String email,
        String fullName,
        boolean emailVerified,
        List<String> roles,
        Instant createdAt,
        Long currentGroupId,
        List<GroupInfoResponse> groups
) {}

