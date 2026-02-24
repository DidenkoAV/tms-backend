package com.test.system.service.jira;

import com.test.system.client.JiraHttpClient;
import com.test.system.component.jira.JiraAttachmentUploader;
import com.test.system.component.jira.JiraIssueMapper;
import com.test.system.component.jira.JiraPayloadBuilder;
import com.test.system.component.jira.JiraResponseParser;
import com.test.system.dto.jira.issue.CreateIssueRequest;
import com.test.system.dto.jira.issue.JiraIssueDetailsResponse;
import com.test.system.dto.jira.issue.TestCaseIssueResponse;
import com.test.system.dto.jira.response.JiraCreateIssueApiResponse;
import com.test.system.dto.jira.response.JiraCreateMetadataApiResponse;
import com.test.system.model.cases.TestCase;
import com.test.system.model.jira.JiraConnection;
import com.test.system.model.jira.TestCaseIssue;
import com.test.system.repository.jira.TestCaseIssueRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages Jira issues linked to test cases and provides metadata operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraIssueService {

    private final JiraConnectionService connectionService;
    private final TestCaseIssueRepository issueLinks;
    private final TestCaseRepository testCases;
    private final JiraHttpClient httpClient;
    private final JiraPayloadBuilder payloadBuilder;
    private final JiraIssueMapper mapper;
    private final JiraAttachmentUploader uploader;
    private final JiraResponseParser parser;

    /**
     * Creates a new Jira issue and links it to a test case.
     */
    public TestCaseIssueResponse createJiraIssue(Long groupId, Long testCaseId, CreateIssueRequest req) {
        log.info("[JiraIssue] Create: groupId={}, tcId={}, summary={}", groupId, testCaseId, req.summary());

        JiraConnection conn = connectionService.getConnectionEntity(groupId);
        TestCase testCase = loadTestCase(testCaseId);

        String payload = payloadBuilder.buildCreateIssuePayload(conn, req);
        JiraCreateIssueApiResponse response = httpClient.createJiraIssue(conn, payload);

        uploader.uploadIfPresent(conn, response.key(), req.attachments());

        TestCaseIssue saved = saveIssueLink(testCase, conn, response.key());

        log.info("[JiraIssue] Created: key={}, linkId={}", response.key(), saved.getId());
        return toDto(groupId, saved);
    }

    /**
     * Attaches existing Jira issue to a test case.
     */
    public TestCaseIssueResponse attachJiraIssue(Long groupId, Long testCaseId, String issueKey) {
        log.info("[JiraIssue] Attach: groupId={}, tcId={}, key={}", groupId, testCaseId, issueKey);

        JiraConnection conn = connectionService.getConnectionEntity(groupId);
        TestCase testCase = loadTestCase(testCaseId);

        String normalizedKey = issueKey.trim().toUpperCase();
        verifyIssueExists(conn, normalizedKey);

        TestCaseIssue saved = saveIssueLink(testCase, conn, normalizedKey);

        log.info("[JiraIssue] Attached: key={}, linkId={}", normalizedKey, saved.getId());
        return toDto(groupId, saved);
    }

    /**
     * Saves issue link in a separate transaction to ensure it's committed
     * even if subsequent operations (like fetching details from Jira) fail.
     */
    @Transactional
    private TestCaseIssue saveIssueLink(TestCase testCase, JiraConnection conn, String issueKey) {
        TestCaseIssue link = buildLink(testCase, conn, issueKey);
        return issueLinks.save(link);
    }

    /**
     * Lists all issues linked to a test case.
     */
    @Transactional
    public List<TestCaseIssueResponse> listJiraIssues(Long groupId, Long testCaseId) {
        log.info("[JiraIssue] List: groupId={}, tcId={}", groupId, testCaseId);
        cleanupBrokenLinks(groupId, testCaseId);

      return getIssuesForTestCase(testCaseId).stream()
              .map(link -> toDto(groupId, link))
              .toList();
    }

    /**
     * Detaches issue from test case.
     */
    @Transactional
    public void detachJiraIssue(Long linkId) {
        log.warn("[JiraIssue] Detach: linkId={}", linkId);
        issueLinks.deleteById(linkId);
    }

    /**
     * Removes broken links (issues deleted in Jira).
     */
    @Transactional
    public void cleanupBrokenLinks(Long groupId, Long testCaseId) {
        var links = getIssuesForTestCase(testCaseId);

        for (var link : links) {
            try {
                getIssueDetails(groupId, link.getIssueKey());
            } catch (RuntimeException e) {
                issueLinks.deleteById(link.getId());
            }
        }
    }

    /**
     * Gets available issue types for a project.
     */
    public List<String> getAvailableIssueTypes(Long groupId) {
        log.info("[JiraIssue] GetTypes: groupId={}", groupId);
        JiraConnection conn = connectionService.getConnectionEntity(groupId);

        JiraCreateMetadataApiResponse response = httpClient.getJiraCreateMetadata(conn, conn.getDefaultProject());
        return parser.parseIssueTypes(response);
    }

    /**
     * Gets detailed information about a Jira issue.
     */
    public JiraIssueDetailsResponse getIssueDetails(Long groupId, String issueKey) {
        log.info("[JiraIssue] GetDetails: groupId={}, key={}", groupId, issueKey);
        JiraConnection conn = connectionService.getConnectionEntity(groupId);
        return httpClient.getJiraIssueDetails(conn, issueKey);
    }

    private List<TestCaseIssue> getIssuesForTestCase(Long testCaseId) {
        return issueLinks.findByTestCaseId(testCaseId);
    }

    private TestCaseIssueResponse toDto(Long groupId, TestCaseIssue issue) {
        var details = getIssueDetails(groupId, issue.getIssueKey());
        return mapper.toIssueDto(issue, details);
    }

    private TestCase loadTestCase(Long testCaseId) {
        return testCases.findById(testCaseId)
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found: " + testCaseId));
    }

    private void verifyIssueExists(JiraConnection conn, String issueKey) {
        if (!httpClient.jiraIssueExists(conn, issueKey)) {
            throw new EntityNotFoundException("Issue " + issueKey + " not found in Jira");
        }
    }

    private TestCaseIssue buildLink(TestCase testCase, JiraConnection conn, String issueKey) {
        return TestCaseIssue.builder()
                .testCase(testCase)
                .issueKey(issueKey)
                .issueUrl(mapper.buildIssueUrl(conn, issueKey))
                .build();
    }
}

