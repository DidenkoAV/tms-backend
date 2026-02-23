package com.test.system.dto.authorization.token;

import java.time.Instant;
import java.util.UUID;

public record ApiTokenResponse(
        UUID id,
        String name,
        Instant createdAt,
        Instant lastUsedAt,
        Boolean revoked,
        String tokenOnce // отдаём только при создании
) {}

