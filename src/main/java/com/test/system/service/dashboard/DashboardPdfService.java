package com.test.system.service.dashboard;

import com.test.system.dto.dashboard.DashboardData;
import com.test.system.dto.dashboard.DashboardExportRequest;
import com.test.system.model.cases.TestCase;
import com.test.system.model.milestone.Milestone;
import com.test.system.model.project.Project;
import com.test.system.model.run.Run;
import com.test.system.model.suite.Suite;
import com.test.system.repository.milestone.MilestoneRepository;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.run.TestResultRepository;
import com.test.system.repository.run.TestRunRepository;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardPdfService {

    private final TemplateEngine templateEngine;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final TestSuiteRepository testSuiteRepository;
    private final MilestoneRepository milestoneRepository;

    @Transactional(readOnly = true)
    public byte[] generateDashboardPdf(DashboardExportRequest request) {
        log.info("Generating dashboard PDF for {} projects", request.projectIds().size());
        
        // 1. Collect data from database
        DashboardData dashboardData = collectDashboardData(request.projectIds());
        
        // 2. Generate HTML from template
        String html = generateHtmlFromTemplate(dashboardData);
        
        // 3. Convert HTML to PDF
        return convertHtmlToPdf(html);
    }

    private DashboardData collectDashboardData(List<Long> projectIds) {
        log.debug("Collecting dashboard data for project IDs: {}", projectIds);
        
        // Fetch all projects
        List<Project> projects = projectRepository.findAllById(projectIds);
        
        // Fetch all test cases for these projects
        List<TestCase> allCases = testCaseRepository.findByProjectIdIn(projectIds);
        
        // Fetch all test runs
        List<Run> allRuns = testRunRepository.findByProjectIdIn(projectIds);

        // Fetch all test suites
        List<Suite> allSuites = testSuiteRepository.findByProjectIdIn(projectIds);
        
        // Fetch all milestones
        List<Milestone> allMilestones = milestoneRepository.findByProjectIdIn(projectIds);
        
        // Calculate statistics
        DashboardData.DashboardStats stats = calculateStats(projects, allCases, allRuns, allSuites, allMilestones);
        
        // Build chart data
        List<DashboardData.TestCaseStatusData> testStatusData = buildTestStatusData(allCases);
        List<DashboardData.AutomationStatusData> automationStatusData = buildAutomationStatusData(allCases);
        List<DashboardData.PriorityData> priorityData = buildPriorityData(allCases);
        List<DashboardData.ProjectActivityData> projectActivityData = buildProjectActivityData(projects, allCases, allRuns, allSuites);
        List<DashboardData.RecentRunData> recentRuns = buildRecentRunsData(allRuns);
        
        return DashboardData.builder()
                .stats(stats)
                .testStatusData(testStatusData)
                .automationStatusData(automationStatusData)
                .priorityData(priorityData)
                .projectActivityData(projectActivityData)
                .recentRuns(recentRuns)
                .build();
    }

    private DashboardData.DashboardStats calculateStats(
            List<Project> projects,
            List<TestCase> allCases,
            List<Run> allRuns,
            List<Suite> allSuites,
            List<Milestone> allMilestones) {

        int totalCases = allCases.size();
        int passedCases = (int) allCases.stream()
                .filter(tc -> TestCase.Status.PASSED.equals(tc.getStatus()))
                .count();
        int automatedCases = (int) allCases.stream()
                .filter(tc -> TestCase.AutomationStatus.AUTOMATED.equals(tc.getAutomationStatus()))
                .count();
        int activeRuns = (int) allRuns.stream()
                .filter(run -> !run.isClosed())
                .count();
        int closedRuns = (int) allRuns.stream()
                .filter(Run::isClosed)
                .count();

        double passRate = totalCases > 0 ? (passedCases * 100.0 / totalCases) : 0.0;
        double automationRate = totalCases > 0 ? (automatedCases * 100.0 / totalCases) : 0.0;

        return DashboardData.DashboardStats.builder()
                .totalProjects(projects.size())
                .totalCases(totalCases)
                .totalRuns(allRuns.size())
                .totalSuites(allSuites.size())
                .totalMilestones(allMilestones.size())
                .passRate(Math.round(passRate * 10) / 10.0)
                .automationRate(Math.round(automationRate * 10) / 10.0)
                .activeRuns(activeRuns)
                .closedRuns(closedRuns)
                .build();
    }

    private List<DashboardData.TestCaseStatusData> buildTestStatusData(List<TestCase> allCases) {
        Map<String, Long> statusCounts = allCases.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getStatus() != null ? tc.getStatus().name() : "UNKNOWN",
                        Collectors.counting()
                ));

        return statusCounts.entrySet().stream()
                .map(entry -> DashboardData.TestCaseStatusData.builder()
                        .status(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardData.AutomationStatusData> buildAutomationStatusData(List<TestCase> allCases) {
        Map<String, Long> automationCounts = allCases.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getAutomationStatus() != null ? tc.getAutomationStatus().name() : "NOT_AUTOMATED",
                        Collectors.counting()
                ));

        return automationCounts.entrySet().stream()
                .map(entry -> DashboardData.AutomationStatusData.builder()
                        .status(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardData.PriorityData> buildPriorityData(List<TestCase> allCases) {
        // Priority is stored as priorityId (Long), we need to get actual priority names
        // For now, we'll group by priorityId and use it as string
        Map<String, Long> priorityCounts = allCases.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getPriorityId() != null ? "Priority_" + tc.getPriorityId() : "UNKNOWN",
                        Collectors.counting()
                ));

        return priorityCounts.entrySet().stream()
                .map(entry -> DashboardData.PriorityData.builder()
                        .priority(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardData.ProjectActivityData> buildProjectActivityData(
            List<Project> projects,
            List<TestCase> allCases,
            List<Run> allRuns,
            List<Suite> allSuites) {

        return projects.stream()
                .map(project -> {
                    int casesCount = (int) allCases.stream()
                            .filter(tc -> tc.getProjectId().equals(project.getId()))
                            .count();
                    int runsCount = (int) allRuns.stream()
                            .filter(run -> run.getProjectId().equals(project.getId()))
                            .count();
                    int suitesCount = (int) allSuites.stream()
                            .filter(suite -> suite.getProjectId().equals(project.getId()))
                            .count();

                    return DashboardData.ProjectActivityData.builder()
                            .projectName(project.getName())
                            .cases(casesCount)
                            .runs(runsCount)
                            .suites(suitesCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<DashboardData.RecentRunData> buildRecentRunsData(List<Run> allRuns) {
        return allRuns.stream()
                .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
                .limit(10)
                .map(run -> {
                    double passRate = calculateRunPassRate(run);
                    String status = run.isClosed() ? "CLOSED" : "ACTIVE";
                    String date = run.getCreatedAt()
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

                    return DashboardData.RecentRunData.builder()
                            .name(run.getName())
                            .project("Project") // We don't have project reference in Run, need to fetch separately
                            .status(status)
                            .passRate(Math.round(passRate * 10) / 10.0)
                            .date(date)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private double calculateRunPassRate(Run run) {
        // This is a simplified calculation - adjust based on your actual data model
        // You might need to fetch test results for the run
        return 0.0; // Placeholder
    }

    private String generateHtmlFromTemplate(DashboardData data) {
        Context context = new Context();
        context.setVariable("data", data);
        context.setVariable("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

        return templateEngine.process("dashboard-pdf", context);
    }

    private byte[] convertHtmlToPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error converting HTML to PDF", e);
            throw new RuntimeException("Failed to convert HTML to PDF", e);
        }
    }
}

