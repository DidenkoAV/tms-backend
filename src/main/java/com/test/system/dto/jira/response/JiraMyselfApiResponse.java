package com.test.system.dto.jira.response;

/**
 * Raw response from Jira API when testing connection.
 * Maps directly to JSON response from GET /rest/api/3/myself
 */
public record JiraMyselfApiResponse(
        String accountId,
        String emailAddress,
        String displayName
) {
}

