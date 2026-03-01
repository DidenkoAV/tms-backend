package com.test.system.dto.run.response;

public record RunStatusCountResponse(
        Long runId,
        Long statusId,
        Long count
) {
}
