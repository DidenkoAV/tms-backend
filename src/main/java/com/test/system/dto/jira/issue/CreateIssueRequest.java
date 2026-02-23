package com.test.system.dto.jira.issue;

import com.test.system.dto.jira.attachment.AttachmentUploadRequest;

import java.util.List;

public record CreateIssueRequest(
        String summary,
        String description,
        String issueType,
        String priority,
        String author,
        List<AttachmentUploadRequest> attachments
) {}

