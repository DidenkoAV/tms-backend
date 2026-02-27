package com.test.system.dto.jira.response;

import java.util.List;

/**
 * Raw response from Jira API when searching issues using JQL.
 * Maps directly to JSON response from POST /rest/api/3/search
 */
public record JiraSearchApiResponse(
        Integer startAt,
        Integer maxResults,
        Integer total,
        List<JiraIssueApiResponse> issues
) {
}

