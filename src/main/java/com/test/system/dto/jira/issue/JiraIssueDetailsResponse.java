package com.test.system.dto.jira.issue;

import com.test.system.dto.jira.attachment.AttachmentInfoResponse;

import java.util.List;

public record JiraIssueDetailsResponse(
        String status,
        String summary,
        String description,
        String author,
        String priority,
        String issueType,
        List<AttachmentInfoResponse> attachments
) {}

