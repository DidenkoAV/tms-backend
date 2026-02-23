package com.test.system.component.jira;

import com.test.system.dto.jira.issue.CreateIssueRequest;
import com.test.system.model.jira.JiraConnection;
import org.springframework.stereotype.Component;

/**
 * Builds JSON payloads for Jira REST API requests.
 */
@Component
public class JiraPayloadBuilder {

    /**
     * Builds JSON payload for creating a Jira issue.
     */
    public String buildCreateIssuePayload(JiraConnection conn, CreateIssueRequest req) {
        String descriptionText = resolveDescription(req.description());
        String descriptionADF = buildDescriptionAdf(descriptionText);

        return buildCreateIssueJson(
                conn,
                req.summary(),
                descriptionADF,
                req.issueType(),
                req.priority(),
                req.author()
        );
    }

    /**
     * Builds ADF (Atlassian Document Format) for description field.
     */
    public String buildDescriptionAdf(String description) {
        String escaped = escapeJson(description);

        return """
                {
                  "type": "doc",
                  "version": 1,
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        { "type": "text", "text": "%s" }
                      ]
                    }
                  ]
                }
                """.formatted(escaped);
    }

    /**
     * Escapes string for safe JSON embedding.
     */
    public String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String buildCreateIssueJson(JiraConnection conn,
                                        String summary,
                                        String descriptionADF,
                                        String issueType,
                                        String priority,
                                        String author) {

        String projectKey = conn.getDefaultProject();
        String safeSummary = escapeJson(summary);
        String finalIssueType = escapeJson(
                issueType != null && !issueType.isBlank()
                        ? issueType
                        : conn.getDefaultIssueType()
        );
        String finalPriority = escapeJson(
                priority != null && !priority.isBlank()
                        ? priority
                        : "Medium"
        );
        String reporter = escapeJson(
                author != null && !author.isBlank()
                        ? author
                        : conn.getEmail()
        );

        return """
                {
                  "fields": {
                    "project": { "key": "%s" },
                    "summary": "%s",
                    "issuetype": { "name": "%s" },
                    "priority": { "name": "%s" },
                    "description": %s,
                    "reporter": { "name": "%s" }
                  }
                }
                """.formatted(
                projectKey,
                safeSummary,
                finalIssueType,
                finalPriority,
                descriptionADF,
                reporter
        );
    }

    private String resolveDescription(String description) {
        return (description != null && !description.isBlank())
                ? description
                : "Created from TestForge";
    }
}

