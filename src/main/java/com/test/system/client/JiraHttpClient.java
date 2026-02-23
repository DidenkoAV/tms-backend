package com.test.system.client;

import com.test.system.component.jira.JiraResponseParser;
import com.test.system.dto.jira.issue.JiraIssueDetailsResponse;
import com.test.system.dto.jira.response.JiraCreateIssueApiResponse;
import com.test.system.dto.jira.response.JiraCreateMetadataApiResponse;
import com.test.system.dto.jira.response.JiraIssueApiResponse;
import com.test.system.dto.jira.response.JiraMyselfApiResponse;
import com.test.system.exceptions.jira.JiraApiException;
import com.test.system.exceptions.jira.JiraNotFoundException;
import com.test.system.model.jira.JiraConnection;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for Jira REST API communication.
 * Handles authentication, request building, and error handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JiraHttpClient {

    private final RestClient restClient = RestClient.create();
    private final JiraResponseParser parser;

    // ========== Public API Methods ==========


    public boolean testJiraConnection(JiraConnection conn) {
        log.debug("[Jira] Test connection");
        JiraMyselfApiResponse response = get(conn, JiraApiEndpoint.MYSELF.getPath(), JiraMyselfApiResponse.class);
        return response != null && response.accountId() != null && !response.accountId().isEmpty();
    }
    
    public JiraCreateIssueApiResponse createJiraIssue(JiraConnection conn, String jsonPayload) {
        log.debug("[Jira] Create issue");
        return post(conn, JiraApiEndpoint.CREATE_ISSUE.getPath(), jsonPayload, JiraCreateIssueApiResponse.class);
    }
    
    public boolean jiraIssueExists(JiraConnection conn, String issueKey) {
        log.debug("[Jira] Check issue exists: {}", issueKey);
        JiraIssueApiResponse response = get(conn, JiraApiEndpoint.GET_ISSUE.format(issueKey), JiraIssueApiResponse.class);
        return response != null && response.key() != null && !response.key().isEmpty();
    }
    
    public void uploadJiraAttachment(JiraConnection conn, String issueKey, String filename, byte[] fileBytes) {
        log.debug("[Jira] Upload attachment: issue={}, file={}", issueKey, filename);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", createFileResource(filename, fileBytes));

        postMultipart(conn, JiraApiEndpoint.UPLOAD_ATTACHMENT.format(issueKey), body);
    }
    
    public JiraCreateMetadataApiResponse getJiraCreateMetadata(JiraConnection conn, String projectKey) {
        log.debug("[Jira] Get create metadata: project={}", projectKey);

        JiraCreateMetadataApiResponse response = get(
                conn,
                JiraApiEndpoint.CREATE_METADATA.format(projectKey),
                JiraCreateMetadataApiResponse.class
        );

        return response != null ? response : new JiraCreateMetadataApiResponse(List.of());
    }

    public JiraIssueDetailsResponse getJiraIssueDetails(JiraConnection conn, String issueKey) {
        log.debug("[Jira] Get issue details: {}", issueKey);

        JiraIssueApiResponse apiResponse = get(conn, JiraApiEndpoint.GET_ISSUE.format(issueKey), JiraIssueApiResponse.class);

        if (apiResponse == null) {
            throw new EntityNotFoundException("Issue not found: " + issueKey);
        }

        return parser.parseIssueDetails(apiResponse, issueKey);
    }

    // ========== Private HTTP Methods ==========

    private <T> T get(JiraConnection conn, String path, Class<T> responseType) {
        String url = buildUrl(conn, path);
        log.debug("[Jira] GET: {}", path);

        try {
            T response = restClient.get()
                    .uri(url)
                    .header("Authorization", buildBasicAuth(conn))
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .toEntity(responseType)
                    .getBody();

            // Check if response is null
            if (response == null) {
                log.warn("[Jira] GET returned null: {}", path);
            }

            return response;

        } catch (RestClientResponseException e) {
            handleError("GET", path, e);
            return null;
        }
    }

    private <T> T post(JiraConnection conn, String path, String jsonBody, Class<T> responseType) {
        String url = buildUrl(conn, path);
        log.debug("[Jira] POST: {}", path);

        try {
            T response = restClient.post()
                    .uri(url)
                    .header("Authorization", buildBasicAuth(conn))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .toEntity(responseType)
                    .getBody();

            // Check if response is null
            if (response == null) {
                log.warn("[Jira] POST returned null: {}", path);
            }

            return response;

        } catch (RestClientResponseException e) {
            handleError("POST", path, e);
            return null;
        }
    }


    private void postMultipart(JiraConnection conn, String path, Object multipartBody) {
        String url = buildUrl(conn, path);
        log.debug("[Jira] POST multipart: {}", path);

        try {
            restClient.post()
                    .uri(url)
                    .header("Authorization", buildBasicAuth(conn))
                    .header("X-Atlassian-Token", "no-check")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            handleError("POST multipart", path, e);
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Build full URL from connection base URL and path.
     */
    private String buildUrl(JiraConnection conn, String path) {
        return conn.getBaseUrl().replaceAll("/+$", "") + path;
    }

    /**
     * Build Basic Authentication header value.
     */
    private String buildBasicAuth(JiraConnection conn) {
        String credentials = conn.getEmail() + ":" + conn.getTokenEncrypted();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    /**
     * Create file resource for multipart upload.
     */
    private ByteArrayResource createFileResource(String filename, byte[] fileBytes) {
        return new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    /**
     * Handle HTTP errors and throw appropriate exceptions.
     */
    private void handleError(String method, String path, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        log.error("[Jira] {} failed: path={}, status={}", method, path, status);

        if (status == 404) {
            throw new JiraNotFoundException("Resource not found: " + path);
        }

        throw new JiraApiException("API error: " + e.getMessage(), e);
    }
}
