package com.test.system.dto.jira.response;

/**
 * Raw response from Jira API when creating an issue.
 * Maps directly to JSON response from POST /rest/api/3/issue
 */
public record JiraCreateIssueApiResponse(
        String id,
        String key,
        String self
) {
}

