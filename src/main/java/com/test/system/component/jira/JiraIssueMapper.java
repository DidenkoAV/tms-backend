package com.test.system.component.jira;

import com.test.system.dto.jira.connection.JiraConnectionResponse;
import com.test.system.dto.jira.issue.JiraIssueDetailsResponse;
import com.test.system.dto.jira.issue.TestCaseIssueResponse;
import com.test.system.model.jira.JiraConnection;
import com.test.system.model.jira.TestCaseIssue;
import org.springframework.stereotype.Component;

/**
 * Maps Jira entities to DTOs.
 */
@Component
public class JiraIssueMapper {

    /**
     * Maps JiraConnection entity to DTO.
     */
    public JiraConnectionResponse toConnectionDto(JiraConnection conn) {
        return JiraConnectionResponse.from(conn);
    }

    /**
     * Maps TestCaseIssue entity to DTO with issue details.
     */
    public TestCaseIssueResponse toIssueDto(TestCaseIssue issue, JiraIssueDetailsResponse details) {
        return new TestCaseIssueResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getIssueUrl(),
                details.summary(),
                details.description(),
                details.status(),
                details.author(),
                details.priority(),
                details.attachments()
        );
    }

    /**
     * Builds issue URL from connection and issue key.
     */
    public String buildIssueUrl(JiraConnection conn, String issueKey) {
        return conn.getBaseUrl().replaceAll("/+$", "") + "/browse/" + issueKey;
    }
}

