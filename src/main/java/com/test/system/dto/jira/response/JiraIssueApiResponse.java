package com.test.system.dto.jira.response;

import java.util.Map;

/**
 * Raw response from Jira API when getting issue details.
 * Maps directly to JSON response from GET /rest/api/3/issue/{issueKey}
 */
public record JiraIssueApiResponse(
        String id,
        String key,
        String self,
        Map<String, Object> fields
) {
}

