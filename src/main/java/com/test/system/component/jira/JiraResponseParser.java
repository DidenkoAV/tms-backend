package com.test.system.component.jira;

import com.test.system.dto.jira.attachment.AttachmentInfoResponse;
import com.test.system.dto.jira.issue.JiraIssueDetailsResponse;
import com.test.system.dto.jira.response.JiraCreateMetadataApiResponse;
import com.test.system.dto.jira.response.JiraIssueApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses Jira REST API responses into DTOs.
 */
@Component
@RequiredArgsConstructor
public class JiraResponseParser {

    private final JiraAdfConverter adfConverter;

    /**
     * Parses Jira issue API response into domain response.
     */
    @SuppressWarnings("unchecked")
    public JiraIssueDetailsResponse parseIssueDetails(JiraIssueApiResponse apiResponse, String issueKey) {
        if (apiResponse == null) {
            throw new EntityNotFoundException("Issue " + issueKey + " not found");
        }

        Map<String, Object> fields = apiResponse.fields();
        if (fields == null) {
            throw new EntityNotFoundException("Issue " + issueKey + " has no fields");
        }

        String summary = extractString(fields, "summary");
        String status = extractStatus(fields);
        String description = extractDescription(fields);
        String author = extractAuthor(fields);
        String priority = extractPriority(fields);
        String issueType = extractIssueType(fields);
        List<AttachmentInfoResponse> attachments = extractAttachments(fields);

        return new JiraIssueDetailsResponse(status, summary, description, author, priority, issueType, attachments);
    }

    /**
     * Parses available issue types from createmeta API response.
     */
    @SuppressWarnings("unchecked")
    public List<String> parseIssueTypes(JiraCreateMetadataApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.projects() == null || apiResponse.projects().isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> projects = apiResponse.projects();
        Map<String, Object> firstProject = projects.get(0);

        var issueTypes = (List<Map<String, Object>>) firstProject.get("issuetypes");
        if (issueTypes == null) {
            return List.of();
        }

        return issueTypes.stream()
                .map(it -> (String) it.get("name"))
                .filter(name -> name != null && !"Sub-task".equalsIgnoreCase(name))
                .toList();
    }

    private String extractString(Map<String, Object> fields, String key) {
        Object obj = fields.get(key);
        return obj instanceof String s ? s : "";
    }

    @SuppressWarnings("unchecked")
    private String extractStatus(Map<String, Object> fields) {
        Object statusObj = fields.get("status");
        if (statusObj instanceof Map<?, ?> statusMap) {
            Object nameObj = statusMap.get("name");
            return nameObj instanceof String s ? s : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractDescription(Map<String, Object> fields) {
        Object descObj = fields.get("description");
        if (descObj instanceof Map<?, ?> descMap) {
            return adfConverter.extractMarkdown((Map<String, Object>) descMap);
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractAuthor(Map<String, Object> fields) {
        Object reporterObj = fields.get("reporter");
        if (reporterObj instanceof Map<?, ?> reporterMap) {
            Object displayName = reporterMap.get("displayName");
            return displayName instanceof String s ? s : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractPriority(Map<String, Object> fields) {
        Object priorityObj = fields.get("priority");
        if (priorityObj instanceof Map<?, ?> priorityMap) {
            Object nameObj = priorityMap.get("name");
            return nameObj instanceof String s ? s : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private String extractIssueType(Map<String, Object> fields) {
        Object issueTypeObj = fields.get("issuetype");
        if (issueTypeObj instanceof Map<?, ?> issueTypeMap) {
            Object nameObj = issueTypeMap.get("name");
            return nameObj instanceof String s ? s : "";
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private List<AttachmentInfoResponse> extractAttachments(Map<String, Object> fields) {
        List<AttachmentInfoResponse> attachments = new ArrayList<>();
        Object attachmentObj = fields.get("attachment");
        
        if (!(attachmentObj instanceof List<?> attList)) {
            return attachments;
        }

        for (Object o : attList) {
            if (!(o instanceof Map<?, ?> att)) continue;

            String id = extractAttachmentField(att, "id");
            String name = extractAttachmentField(att, "filename");
            String mimeType = extractMimeType(att);
            Long size = extractSize(att);
            String url = extractAttachmentField(att, "content");

            attachments.add(new AttachmentInfoResponse(id, name, mimeType, size, url));
        }

        return attachments;
    }

    private String extractAttachmentField(Map<?, ?> att, String key) {
        Object obj = att.get(key);
        return obj != null ? String.valueOf(obj) : null;
    }

    private String extractMimeType(Map<?, ?> att) {
        Object mime = att.get("mimeType");
        if (!(mime instanceof String)) {
            mime = att.get("mimetype");
        }
        return mime instanceof String s ? s : null;
    }

    private Long extractSize(Map<?, ?> att) {
        Object sizeObj = att.get("size");
        return sizeObj instanceof Number n ? n.longValue() : null;
    }
}

