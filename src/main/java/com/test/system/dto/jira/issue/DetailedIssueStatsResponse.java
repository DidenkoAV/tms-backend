package com.test.system.dto.jira.issue;

import java.util.Map;

/**
 * Detailed statistics for Jira issues grouped by various dimensions.
 */
public record DetailedIssueStatsResponse(
        long total,
        Map<String, Long> byStatus,
        Map<String, Long> byIssueType,
        Map<String, Long> byAuthor,
        Map<String, Long> byPriority
) {}

