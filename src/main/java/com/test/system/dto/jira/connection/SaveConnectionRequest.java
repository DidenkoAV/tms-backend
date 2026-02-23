package com.test.system.dto.jira.connection;

public record SaveConnectionRequest(
        String baseUrl,
        String email,
        String apiToken,
        String defaultProject,
        String defaultIssueType
) {}

