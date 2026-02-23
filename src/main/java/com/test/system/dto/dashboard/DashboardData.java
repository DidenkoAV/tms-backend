package com.test.system.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardData {
    
    // Statistics
    private DashboardStats stats;
    
    // Chart data
    private List<TestCaseStatusData> testStatusData;
    private List<AutomationStatusData> automationStatusData;
    private List<PriorityData> priorityData;
    private List<ProjectActivityData> projectActivityData;
    private List<TestExecutionTrendData> executionTrendData;
    private List<SeverityData> severityData;
    
    // Recent runs
    private List<RecentRunData> recentRuns;
    
    @Data
    @Builder
    public static class DashboardStats {
        private int totalProjects;
        private int totalCases;
        private int totalRuns;
        private int totalSuites;
        private int totalMilestones;
        private double passRate;
        private double automationRate;
        private int activeRuns;
        private int closedRuns;
    }
    
    @Data
    @Builder
    public static class TestCaseStatusData {
        private String status;
        private int count;
    }
    
    @Data
    @Builder
    public static class AutomationStatusData {
        private String status;
        private int count;
    }
    
    @Data
    @Builder
    public static class PriorityData {
        private String priority;
        private int count;
    }
    
    @Data
    @Builder
    public static class ProjectActivityData {
        private String projectName;
        private int cases;
        private int runs;
        private int suites;
    }
    
    @Data
    @Builder
    public static class TestExecutionTrendData {
        private String date;
        private int passed;
        private int failed;
    }
    
    @Data
    @Builder
    public static class SeverityData {
        private String severity;
        private int passed;
        private int failed;
    }
    
    @Data
    @Builder
    public static class RecentRunData {
        private String name;
        private String project;
        private String status;
        private double passRate;
        private String date;
    }
}

