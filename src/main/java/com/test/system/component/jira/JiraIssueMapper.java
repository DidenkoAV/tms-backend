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
     * Maps TestCaseIssue entity to DTO with issue details from Jira.
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
     * Maps TestCaseIssue entity to lightweight DTO without fetching Jira details.
     * Use this for list views to avoid excessive Jira API calls.
     */
    public TestCaseIssueResponse toIssueDtoLightweight(TestCaseIssue issue) {
        return new TestCaseIssueResponse(
                issue.getId(),
                issue.getIssueKey(),
                issue.getIssueUrl(),
                null,  // summary - not available without Jira call
                null,  // description - not available without Jira call
                issue.getStatus(),  // cached status from DB
                null,  // author - not available without Jira call
                null,  // priority - not available without Jira call
                null   // attachments - not available without Jira call
        );
    }

    /**
     * Builds issue URL from connection and issue key.
     */
    public String buildIssueUrl(JiraConnection conn, String issueKey) {
        return conn.getBaseUrl().replaceAll("/+$", "") + "/browse/" + issueKey;
    }
}

