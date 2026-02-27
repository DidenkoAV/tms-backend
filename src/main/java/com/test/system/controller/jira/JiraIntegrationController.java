package com.test.system.controller.jira;

import com.test.system.dto.jira.connection.JiraConnectionResponse;
import com.test.system.dto.jira.connection.SaveConnectionRequest;
import com.test.system.dto.jira.issue.CreateIssueRequest;
import com.test.system.dto.jira.issue.DetailedIssueStatsResponse;
import com.test.system.dto.jira.issue.ProjectIssueStatsResponse;
import com.test.system.dto.jira.issue.TestCaseIssueResponse;
import com.test.system.service.jira.JiraConnectionService;
import com.test.system.service.jira.JiraIssueService;
import com.test.system.service.jira.JiraProjectStatsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Jira Integration Controller", description = "Jira integration and issue management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/integrations/jira")
@RequiredArgsConstructor
public class JiraIntegrationController {

    private final JiraConnectionService connectionService;
    private final JiraIssueService issueService;
    private final JiraProjectStatsService projectStatsService;


    @GetMapping("/connection/{groupId}")
    public JiraConnectionResponse getJiraConnection(@PathVariable Long groupId) {
        return connectionService.getJiraConnection(groupId);
    }

    @PostMapping("/connection/{groupId}")
    public JiraConnectionResponse saveJiraConnection(
            @PathVariable Long groupId,
            @RequestBody SaveConnectionRequest input
    ) {
        return connectionService.saveJiraConnection(groupId, input);
    }

    @DeleteMapping("/connection/{groupId}")
    public void removeJiraConnection(@PathVariable Long groupId) {
        connectionService.removeJiraConnection(groupId);
    }

    @PostMapping("/test/{groupId}")
    public Map<String, String> testJiraConnection(@PathVariable Long groupId) {
        String message = connectionService.testJiraConnection(groupId);
        return Map.of("message", message);
    }


    @PostMapping("/issue/{groupId}/{testCaseId}")
    public TestCaseIssueResponse createJiraIssue(
            @PathVariable Long groupId,
            @PathVariable Long testCaseId,
            @RequestBody CreateIssueRequest body
    ) {
        return issueService.createJiraIssue(groupId, testCaseId, body);
    }

    @PostMapping("/issue/attach/{groupId}/{testCaseId}")
    public TestCaseIssueResponse attachJiraIssue(
            @PathVariable Long groupId,
            @PathVariable Long testCaseId,
            @RequestBody Map<String, String> body
    ) {
        String issueKey = body.get("issueKey");
        return issueService.attachJiraIssue(groupId, testCaseId, issueKey);
    }

    @GetMapping("/issues/{groupId}/{testCaseId}")
    public List<TestCaseIssueResponse> listJiraIssues(
            @PathVariable Long groupId,
            @PathVariable Long testCaseId,
            @RequestParam(name = "withDetails", defaultValue = "true") boolean withDetails
    ) {
        if (withDetails) {
            return issueService.listJiraIssuesWithDetails(groupId, testCaseId);
        } else {
            return issueService.listJiraIssues(groupId, testCaseId);
        }
    }

    /**
     * Batch endpoint to get Jira issues for multiple test cases in one request.
     * This is much more efficient than calling /issues/{groupId}/{testCaseId} for each test case.
     *
     * @param groupId the group ID
     * @param testCaseIds list of test case IDs
     * @return Map where key is testCaseId and value is list of Jira issues for that test case
     */
    @PostMapping("/issues/batch/{groupId}")
    public Map<Long, List<TestCaseIssueResponse>> listJiraIssuesBatch(
            @PathVariable Long groupId,
            @RequestBody List<Long> testCaseIds
    ) {
        return issueService.listJiraIssuesBatch(groupId, testCaseIds);
    }

    @GetMapping("/issue-types/{groupId}")
    public List<String> getJiraIssueTypes(@PathVariable Long groupId) {
        return issueService.getAvailableIssueTypes(groupId);
    }

    @DeleteMapping("/issues/unlink/{id}")
    public void detachJiraIssue(@PathVariable Long id) {
        issueService.detachJiraIssue(id);
    }

    @GetMapping("/projects-stats/{groupId}")
    public ProjectIssueStatsResponse getProjectsStats(
            @PathVariable Long groupId,
            @RequestParam(name = "projectIds", required = false) List<Long> projectIds
    ) {
        return projectStatsService.getProjectsStats(groupId, projectIds);
    }

    @GetMapping("/projects-stats-detailed/{groupId}")
    public DetailedIssueStatsResponse getDetailedProjectsStats(
            @PathVariable Long groupId,
            @RequestParam(name = "projectIds", required = false) List<Long> projectIds
    ) {
        return projectStatsService.getDetailedProjectsStats(groupId, projectIds);
    }

}
