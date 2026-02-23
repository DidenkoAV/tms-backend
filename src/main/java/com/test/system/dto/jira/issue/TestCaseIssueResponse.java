package com.test.system.dto.jira.issue;

import com.test.system.dto.jira.attachment.AttachmentInfoResponse;

import java.util.List;

public record TestCaseIssueResponse(
        Long id,
        String issueKey,
        String issueUrl,
        String summary,
        String description,
        String status,
        String author,
        String priority,
        List<AttachmentInfoResponse> attachments
) {}

