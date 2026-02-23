package com.test.system.component.jira;

import com.test.system.client.JiraHttpClient;
import com.test.system.dto.jira.attachment.AttachmentUploadRequest;
import com.test.system.model.jira.JiraConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * Uploads attachments to Jira issues.
 */
@Component
@RequiredArgsConstructor
public class JiraAttachmentUploader {

    private final JiraHttpClient jiraHttpClient;

    /**
     * Uploads attachments to a Jira issue if any are provided.
     */
    public void uploadIfPresent(JiraConnection conn, String issueKey, List<AttachmentUploadRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        uploadAll(conn, issueKey, attachments);
    }

    /**
     * Uploads all attachments to a Jira issue.
     */
    public void uploadAll(JiraConnection conn, String issueKey, List<AttachmentUploadRequest> attachments) {
        for (AttachmentUploadRequest att : attachments) {
            uploadSingle(conn, issueKey, att);
        }
    }

    private void uploadSingle(JiraConnection conn, String issueKey, AttachmentUploadRequest att) {
        byte[] fileBytes = Base64.getDecoder().decode(att.contentBase64());
        jiraHttpClient.uploadJiraAttachment(conn, issueKey, att.name(), fileBytes);
    }
}

