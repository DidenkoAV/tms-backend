package com.test.system.dto.authorization.profile;

public record UpdateProfileResponse(
        String status,
        boolean nameUpdated,
        boolean emailChangeStarted,
        UserProfileResponse me
) {}

