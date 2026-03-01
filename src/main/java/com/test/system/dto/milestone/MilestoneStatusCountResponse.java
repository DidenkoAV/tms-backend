package com.test.system.dto.milestone;

public record MilestoneStatusCountResponse(
        Long milestoneId,
        Long statusId,
        Long count
) {
}
