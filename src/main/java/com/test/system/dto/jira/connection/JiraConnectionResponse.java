package com.test.system.dto.jira.connection;

import com.test.system.model.jira.JiraConnection;

public record JiraConnectionResponse(
        Long id,
        String baseUrl,
        String email,
        String defaultProject,
        String defaultIssueType,
        boolean hasToken
) {
    public static JiraConnectionResponse from(JiraConnection conn) {
        return new JiraConnectionResponse(
                conn.getId(),
                conn.getBaseUrl(),
                conn.getEmail(),
                conn.getDefaultProject(),
                conn.getDefaultIssueType(),
                conn.getTokenEncrypted() != null
        );
    }
}

