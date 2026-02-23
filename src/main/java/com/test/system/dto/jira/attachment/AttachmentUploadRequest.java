package com.test.system.dto.jira.attachment;

public record AttachmentUploadRequest(
        String name,
        String contentBase64
) {}

