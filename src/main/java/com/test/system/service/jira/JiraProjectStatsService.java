package com.test.system.service.jira;

import com.test.system.dto.jira.issue.ProjectIssueStatsResponse;
import com.test.system.model.cases.TestCase;
import com.test.system.model.jira.TestCaseIssue;
import com.test.system.repository.jira.TestCaseIssueRepository;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Calculates Jira issue statistics for projects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraProjectStatsService {

    private final JiraIssueService issueService;
    private final TestCaseIssueRepository issueLinks;
    private final TestCaseRepository testCases;
    private final ProjectRepository projects;

    /**
     * Gets issue stats for one or all projects in a group.
     */
    public ProjectIssueStatsResponse getProjectsStats(Long groupId, List<Long> projectIds) {
        log.info("[JiraProjectStats] Get: groupId={}, projects={}", groupId, projectIds == null ? "ALL" : projectIds);

        ProjectIssueStatsResponse stats;
        if (projectIds == null || projectIds.isEmpty()) {
            stats = getAllProjectsStats(groupId);
        } else {
            stats = getMultipleProjectsStats(groupId, projectIds);
        }

        log.info("[JiraProjectStats] Result: total={}, statuses={}", stats.total(), stats.statuses().size());
        return stats;
    }

    /**
     * Gets stats for multiple specific projects.
     */
    public ProjectIssueStatsResponse getMultipleProjectsStats(Long groupId, List<Long> projectIds) {
        var statsList = projectIds.stream()
                .map(pid -> getProjectStats(groupId, pid))
                .toList();

        return mergeStats(statsList);
    }

    /**
     * Gets stats for all projects in a group.
     */
    public ProjectIssueStatsResponse getAllProjectsStats(Long groupId) {
        var projectList = projects.findAllActiveByGroupId(groupId);

        if (projectList.isEmpty()) {
            return new ProjectIssueStatsResponse(0, Map.of());
        }

        List<ProjectIssueStatsResponse> statsList = projectList.stream()
                .map(p -> getProjectStats(groupId, p.getId()))
                .toList();

        return mergeStats(statsList);
    }

    /**
     * Gets stats for a single project.
     */
    public ProjectIssueStatsResponse getProjectStats(Long groupId, Long projectId) {
        List<TestCase> cases = testCases.findAllByProjectId(projectId);
        if (cases.isEmpty()) {
            return new ProjectIssueStatsResponse(0, Map.of());
        }

        List<TestCaseIssue> allIssues = issueLinks.findByTestCaseIn(cases);

        Map<String, Long> statusCounts = allIssues.stream()
                .map(issue -> resolveStatus(groupId, issue.getIssueKey()))
                .filter(status -> status != null && !status.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return new ProjectIssueStatsResponse(allIssues.size(), statusCounts);
    }

    private String resolveStatus(Long groupId, String issueKey) {
        try {
            return issueService.getIssueDetails(groupId, issueKey).status();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private ProjectIssueStatsResponse mergeStats(List<ProjectIssueStatsResponse> statsList) {
        long total = 0;
        Map<String, Long> statuses = new HashMap<>();

        for (var s : statsList) {
            total += s.total();
            for (var entry : s.statuses().entrySet()) {
                statuses.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }

        return new ProjectIssueStatsResponse(total, statuses);
    }
}

