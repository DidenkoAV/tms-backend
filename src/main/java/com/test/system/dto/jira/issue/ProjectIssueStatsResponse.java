package com.test.system.dto.jira.issue;

import java.util.Map;

public record ProjectIssueStatsResponse(
        long total,
        Map<String, Long> statuses
) {}

