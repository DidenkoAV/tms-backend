package com.test.system.service.jira;

import com.test.system.dto.jira.issue.DetailedIssueStatsResponse;
import com.test.system.dto.jira.issue.JiraIssueDetailsResponse;
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
     * OPTIMIZED: Uses batch fetching to get all issue details in ONE Jira API call.
     */
    public ProjectIssueStatsResponse getProjectStats(Long groupId, Long projectId) {
        List<TestCase> cases = testCases.findAllByProjectId(projectId);
        if (cases.isEmpty()) {
            return new ProjectIssueStatsResponse(0, Map.of());
        }

        List<TestCaseIssue> allIssues = issueLinks.findByTestCaseIn(cases);
        if (allIssues.isEmpty()) {
            return new ProjectIssueStatsResponse(0, Map.of());
        }

        // OPTIMIZATION: Batch fetch all issue details in ONE API call
        List<String> issueKeys = allIssues.stream()
                .map(TestCaseIssue::getIssueKey)
                .toList();

        Map<String, JiraIssueDetailsResponse> detailsMap = issueService.getIssueDetailsBatch(groupId, issueKeys);

        // Count statuses
        Map<String, Long> statusCounts = detailsMap.values().stream()
                .map(JiraIssueDetailsResponse::status)
                .filter(status -> status != null && !status.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return new ProjectIssueStatsResponse(allIssues.size(), statusCounts);
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

    /**
     * Gets detailed issue stats for one or all projects in a group.
     */
    public DetailedIssueStatsResponse getDetailedProjectsStats(Long groupId, List<Long> projectIds) {
        log.info("[JiraProjectStats] GetDetailed: groupId={}, projects={}", groupId, projectIds == null ? "ALL" : projectIds);

        DetailedIssueStatsResponse stats;
        if (projectIds == null || projectIds.isEmpty()) {
            stats = getAllProjectsDetailedStats(groupId);
        } else {
            stats = getMultipleProjectsDetailedStats(groupId, projectIds);
        }

        log.info("[JiraProjectStats] DetailedResult: total={}, types={}, authors={}",
                stats.total(), stats.byIssueType().size(), stats.byAuthor().size());
        return stats;
    }

    /**
     * Gets detailed stats for multiple specific projects.
     */
    public DetailedIssueStatsResponse getMultipleProjectsDetailedStats(Long groupId, List<Long> projectIds) {
        var statsList = projectIds.stream()
                .map(pid -> getProjectDetailedStats(groupId, pid))
                .toList();

        return mergeDetailedStats(statsList);
    }

    /**
     * Gets detailed stats for all projects in a group.
     */
    public DetailedIssueStatsResponse getAllProjectsDetailedStats(Long groupId) {
        var projectList = projects.findAllActiveByGroupId(groupId);

        if (projectList.isEmpty()) {
            return new DetailedIssueStatsResponse(0, Map.of(), Map.of(), Map.of(), Map.of());
        }

        List<DetailedIssueStatsResponse> statsList = projectList.stream()
                .map(p -> getProjectDetailedStats(groupId, p.getId()))
                .toList();

        return mergeDetailedStats(statsList);
    }

    /**
     * Gets detailed stats for a single project.
     * OPTIMIZED: Uses batch fetching to get all issue details in ONE Jira API call.
     */
    public DetailedIssueStatsResponse getProjectDetailedStats(Long groupId, Long projectId) {
        List<TestCase> cases = testCases.findAllByProjectId(projectId);
        if (cases.isEmpty()) {
            return new DetailedIssueStatsResponse(0, Map.of(), Map.of(), Map.of(), Map.of());
        }

        List<TestCaseIssue> allIssues = issueLinks.findByTestCaseIn(cases);
        if (allIssues.isEmpty()) {
            return new DetailedIssueStatsResponse(0, Map.of(), Map.of(), Map.of(), Map.of());
        }

        // OPTIMIZATION: Batch fetch all issue details in ONE API call
        List<String> issueKeys = allIssues.stream()
                .map(TestCaseIssue::getIssueKey)
                .toList();

        Map<String, JiraIssueDetailsResponse> detailsMap = issueService.getIssueDetailsBatch(groupId, issueKeys);
        List<JiraIssueDetailsResponse> issueDetails = new java.util.ArrayList<>(detailsMap.values());

        // Calculate statistics
        Map<String, Long> statusCounts = issueDetails.stream()
                .map(JiraIssueDetailsResponse::status)
                .filter(status -> status != null && !status.isBlank())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Long> typeCounts = issueDetails.stream()
                .map(JiraIssueDetailsResponse::issueType)
                .filter(type -> type != null && !type.isBlank())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        Map<String, Long> authorCounts = issueDetails.stream()
                .map(JiraIssueDetailsResponse::author)
                .filter(author -> author != null && !author.isBlank())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));

        Map<String, Long> priorityCounts = issueDetails.stream()
                .map(JiraIssueDetailsResponse::priority)
                .filter(priority -> priority != null && !priority.isBlank())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        return new DetailedIssueStatsResponse(
                allIssues.size(),
                statusCounts,
                typeCounts,
                authorCounts,
                priorityCounts
        );
    }

    private DetailedIssueStatsResponse mergeDetailedStats(List<DetailedIssueStatsResponse> statsList) {
        long total = 0;
        Map<String, Long> statuses = new HashMap<>();
        Map<String, Long> types = new HashMap<>();
        Map<String, Long> authors = new HashMap<>();
        Map<String, Long> priorities = new HashMap<>();

        for (var s : statsList) {
            total += s.total();

            for (var entry : s.byStatus().entrySet()) {
                statuses.merge(entry.getKey(), entry.getValue(), Long::sum);
            }

            for (var entry : s.byIssueType().entrySet()) {
                types.merge(entry.getKey(), entry.getValue(), Long::sum);
            }

            for (var entry : s.byAuthor().entrySet()) {
                authors.merge(entry.getKey(), entry.getValue(), Long::sum);
            }

            for (var entry : s.byPriority().entrySet()) {
                priorities.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }

        return new DetailedIssueStatsResponse(total, statuses, types, authors, priorities);
    }
}

