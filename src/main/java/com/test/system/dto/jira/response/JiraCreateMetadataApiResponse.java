package com.test.system.dto.jira.response;

import java.util.List;
import java.util.Map;

/**
 * Raw response from Jira API when getting create metadata.
 * Maps directly to JSON response from GET /rest/api/3/issue/createmeta
 */
public record JiraCreateMetadataApiResponse(
        List<Map<String, Object>> projects
) {
}

