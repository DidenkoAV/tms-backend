package com.test.system.dto.jira.issue;

public record JiraIssueStatusResponse(
        String key,
        Fields fields
) {
    public record Fields(Status status) {}
    
    public record Status(String name) {}
}

