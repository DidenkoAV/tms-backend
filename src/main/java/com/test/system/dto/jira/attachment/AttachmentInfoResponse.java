package com.test.system.dto.jira.attachment;

public record AttachmentInfoResponse(
        String id,
        String name,
        String mimeType,
        Long size,
        String url
) {}

