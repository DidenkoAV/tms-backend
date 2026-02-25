package com.test.system.component.testcase.testrail;

import com.test.system.dto.testcase.common.TestCaseStep;
import com.test.system.dto.testcase.importexport.HierarchicalSuiteImportDto;
import com.test.system.dto.testcase.importexport.SuiteImport;
import com.test.system.dto.testcase.importexport.TestCasesImportRequest;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.dto.testcase.testrail.*;
import com.test.system.enums.testcase.TestCaseAutomationStatus;
import com.test.system.enums.testcase.TestCaseSeverity;
import com.test.system.enums.testcase.TestCaseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Converter for TestRail XML data to internal test case format.
 * Handles hierarchical section structure and custom fields mapping.
 */
@Component
@Slf4j
public class TestRailConverter {

    private static final String LOG_PREFIX = "[TestRailConverter]";
    private static final Pattern STEP_PATTERN = Pattern.compile("\\[STEP\\s+(\\d+)\\]\\s*(.+?)(?=\\[(?:STEP|VERIFY)|$)", Pattern.DOTALL);
    private static final Pattern VERIFY_PATTERN = Pattern.compile("\\[VERIFY\\]\\s*(.+?)(?=\\[(?:STEP|VERIFY)|$)", Pattern.DOTALL);

    /**
     * Convert TestRail suite XML to import request.
     * Creates hierarchical suite structure from TestRail sections.
     *
     * @param suiteXml the TestRail suite XML
     * @param projectId the target project ID
     * @return import request with suites and test cases
     */
    public TestCasesImportRequest convert(TestRailSuiteXml suiteXml, Long projectId) {
        log.info("{} ========== STARTING TESTRAIL CONVERSION ==========", LOG_PREFIX);
        log.info("{} TestRail Suite Name: '{}'", LOG_PREFIX, suiteXml.getName());
        log.info("{} Root Sections Count: {}", LOG_PREFIX,
                suiteXml.getSections() != null ? suiteXml.getSections().size() : 0);

        List<SuiteImport> suites = new ArrayList<>();
        List<TestCaseResponse> cases = new ArrayList<>();

        // Process all root sections
        if (suiteXml.getSections() != null) {
            for (TestRailSectionXml rootSection : suiteXml.getSections()) {
                log.info("{} Processing root section: '{}'", LOG_PREFIX, rootSection.getName());
                processSectionHierarchy(rootSection, null, suites, cases, projectId);
            }
        }

        log.info("{} ========== CONVERSION COMPLETE ==========", LOG_PREFIX);
        log.info("{} Total Suites Created: {}", LOG_PREFIX, suites.size());
        log.info("{} Total Test Cases Created: {}", LOG_PREFIX, cases.size());

        // Log suite hierarchy for debugging
        logSuiteHierarchy(suites);

        return new TestCasesImportRequest(projectId, suites, cases);
    }

    /**
     * Log suite hierarchy for debugging.
     */
    private void logSuiteHierarchy(List<SuiteImport> suites) {
        log.info("{} ========== SUITE HIERARCHY ==========", LOG_PREFIX);

        // Group by parent
        Map<String, List<HierarchicalSuiteImportDto>> byParent = new HashMap<>();
        for (SuiteImport suite : suites) {
            if (suite instanceof HierarchicalSuiteImportDto h) {
                String parentKey = h.parentName() != null ? h.parentName() : "ROOT";
                byParent.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(h);
            }
        }

        // Log root suites first
        List<HierarchicalSuiteImportDto> rootSuites = byParent.getOrDefault("ROOT", List.of());
        for (HierarchicalSuiteImportDto root : rootSuites) {
            logSuiteTree(root, 0, byParent);
        }

        log.info("{} =====================================", LOG_PREFIX);
    }

    /**
     * Recursively log suite tree.
     */
    private void logSuiteTree(
            HierarchicalSuiteImportDto suite,
            int level,
            Map<String, List<HierarchicalSuiteImportDto>> byParent
    ) {
        String indent = "  ".repeat(level);
        log.info("{} {}- {}", LOG_PREFIX, indent, suite.name());

        // Log children
        List<HierarchicalSuiteImportDto> children = byParent.getOrDefault(suite.name(), List.of());
        for (HierarchicalSuiteImportDto child : children) {
            logSuiteTree(child, level + 1, byParent);
        }
    }

    /**
     * Process a section and its children recursively to build hierarchical suite structure.
     *
     * Each section becomes a suite. Child sections become child suites.
     * Test cases are assigned to their immediate parent section.
     *
     * @param section current section to process
     * @param parentSuitePath full path of parent suite (null for root level), e.g., "ui/sync/chrome"
     * @param suites collection to add created suites
     * @param cases collection to add test cases
     * @param projectId target project ID
     */
    private void processSectionHierarchy(
            TestRailSectionXml section,
            String parentSuitePath,
            List<SuiteImport> suites,
            List<TestCaseResponse> cases,
            Long projectId
    ) {
        String sectionName = trimToEmpty(section.getName());

        // Skip sections with empty names
        if (isBlank(sectionName)) {
            log.warn("{} Skipping section with empty name", LOG_PREFIX);
            return;
        }

        // Special handling for "Test Cases" root container - skip creating it but process its children
        boolean isTestCasesRoot = "Test Cases".equals(sectionName) && parentSuitePath == null;

        if (isTestCasesRoot) {
            log.info("{} Skipping creation of root 'Test Cases' container suite, but processing its test cases and children", LOG_PREFIX);
            // Process test cases in this section (C372) - assign to null suite (root level)
            processTestCasesInSection(section, null, cases, projectId);
            // Process child sections directly as root suites (parentSuitePath = null)
            processChildSections(section, null, suites, cases, projectId);
            return;
        }

        // Build full path for this suite
        String currentSuitePath = parentSuitePath != null
                ? parentSuitePath + "/" + sectionName
                : sectionName;

        // Use full parent path for hierarchical import to distinguish between suites with same name
        // For example: "ui/sync/chrome" vs "ui/umh/chrome" - both have children, but different parents
        String parentSuiteKey = parentSuitePath;  // Use full path, not just last component

        // Create suite for this section (if not already exists)
        createSuiteIfNotExists(sectionName, section.getDescription(), parentSuiteKey, suites);

        // Process test cases in this section - use the leaf name, not full path
        processTestCasesInSection(section, sectionName, cases, projectId);

        // Recursively process child sections - pass full path
        processChildSections(section, currentSuitePath, suites, cases, projectId);
    }

    /**
     * Create a suite if it doesn't already exist in the collection.
     *
     * @param suiteName name of the suite to create
     * @param description suite description
     * @param parentSuiteName name of parent suite (null for root)
     * @param suites collection of suites
     */
    private void createSuiteIfNotExists(
            String suiteName,
            String description,
            String parentSuiteName,
            List<SuiteImport> suites
    ) {
        boolean alreadyExists = suites.stream()
                .filter(s -> s instanceof HierarchicalSuiteImportDto)
                .map(s -> (HierarchicalSuiteImportDto) s)
                .anyMatch(h -> h.name().equalsIgnoreCase(suiteName) &&
                              Objects.equals(h.parentName(), parentSuiteName));

        if (!alreadyExists) {
            HierarchicalSuiteImportDto newSuite = new HierarchicalSuiteImportDto(
                    suiteName,
                    trimToEmpty(description),
                    parentSuiteName
            );
            suites.add(newSuite);

            log.debug("{} Created suite: '{}' (parent: '{}')",
                    LOG_PREFIX, suiteName, parentSuiteName != null ? parentSuiteName : "none");
        }
    }

    /**
     * Process all test cases in a section and add them to the cases collection.
     *
     * @param section section containing test cases
     * @param suiteName name of suite to assign test cases to
     * @param cases collection to add test cases
     * @param projectId target project ID
     */
    private void processTestCasesInSection(
            TestRailSectionXml section,
            String suiteName,
            List<TestCaseResponse> cases,
            Long projectId
    ) {
        if (section.getCases() == null || section.getCases().isEmpty()) {
            return;
        }

        log.debug("{} Processing {} test cases in suite '{}'",
                LOG_PREFIX, section.getCases().size(), suiteName);

        for (TestRailCaseXml caseXml : section.getCases()) {
            TestCaseResponse testCase = convertTestCase(caseXml, suiteName, projectId);
            if (testCase != null) {
                cases.add(testCase);
            }
        }
    }

    /**
     * Process all child sections recursively.
     *
     * @param parentSection parent section containing child sections
     * @param parentSuitePath full path of parent suite
     * @param suites collection to add created suites
     * @param cases collection to add test cases
     * @param projectId target project ID
     */
    private void processChildSections(
            TestRailSectionXml parentSection,
            String parentSuitePath,
            List<SuiteImport> suites,
            List<TestCaseResponse> cases,
            Long projectId
    ) {
        if (parentSection.getSections() == null || parentSection.getSections().isEmpty()) {
            return;
        }

        for (TestRailSectionXml childSection : parentSection.getSections()) {
            processSectionHierarchy(childSection, parentSuitePath, suites, cases, projectId);
        }
    }

    /**
     * Convert a single TestRail test case to internal format.
     *
     * @param caseXml TestRail case XML data
     * @param suiteName name of suite this test case belongs to
     * @param projectId target project ID
     * @return converted test case, or null if invalid
     */
    private TestCaseResponse convertTestCase(
            TestRailCaseXml caseXml,
            String suiteName,
            Long projectId
    ) {
        String title = trimToEmpty(caseXml.getTitle());
        if (isBlank(title)) {
            log.warn("{} Skipping test case with empty title", LOG_PREFIX);
            return null;
        }

        log.debug("{} Converting test case: '{}' (suite: '{}')", LOG_PREFIX, title, suiteName);

        TestRailCustomFieldsXml custom = caseXml.getCustom();

        String preconditions = extractPreconditions(custom);
        List<TestCaseStep> steps = extractSteps(custom);
        Map<String, String> autotestMapping = extractAutotestMapping(custom);
        TestCaseAutomationStatus automationStatus = determineAutomationStatus(custom);
        List<String> tags = extractTags(caseXml.getReferences());

        log.debug("{} Test case '{}': preconditions={}, steps={}, automation={}, tags={}",
                LOG_PREFIX, title,
                preconditions != null ? "YES" : "NO",
                steps.size(),
                automationStatus,
                tags.size());

        return new TestCaseResponse(
                null,                                   // id - will be generated
                projectId,
                null,                                   // suiteId - will be resolved by suite name
                suiteName,
                title,
                null,                                   // typeId - will be resolved by type name
                mapTypeToInternal(caseXml.getType()),
                null,                                   // priorityId - will be resolved by priority name
                mapPriorityToInternal(caseXml.getPriority()),
                0,                                      // estimateSeconds
                preconditions,
                0,                                      // sortIndex
                false,                                  // archived
                null,                                   // expectedResult
                null,                                   // actualResult
                null,                                   // testData
                steps,
                List.of(),                              // attachments
                TestCaseStatus.DRAFT,
                TestCaseSeverity.NORMAL,
                automationStatus,
                tags,
                autotestMapping,
                Instant.now(),                          // createdAt
                Instant.now(),                          // updatedAt
                null,                                   // createdBy
                null,                                   // createdByName
                null                                    // createdByEmail
        );
    }

    /**
     * Extract preconditions from custom fields.
     */
    private String extractPreconditions(TestRailCustomFieldsXml custom) {
        return custom != null ? trimToNull(custom.getPreconditions()) : null;
    }

    /**
     * Extract and parse test steps from custom fields.
     */
    private List<TestCaseStep> extractSteps(TestRailCustomFieldsXml custom) {
        if (custom == null) {
            log.debug("{} No custom fields provided for steps extraction", LOG_PREFIX);
            return List.of();
        }

        String stepsText = trimToNull(custom.getSteps());
        if (stepsText == null) {
            log.debug("{} Steps field is empty in custom fields", LOG_PREFIX);
            return List.of();
        }

        log.debug("{} Extracting steps from custom fields, text length: {}", LOG_PREFIX, stepsText.length());
        return parseSteps(stepsText);
    }

    /**
     * Extract autotest mapping from custom fields.
     */
    private Map<String, String> extractAutotestMapping(TestRailCustomFieldsXml custom) {
        return buildAutotestMapping(custom);
    }

    /**
     * Extract tags from references field.
     */
    private List<String> extractTags(String references) {
        return parseTags(references);
    }

    /**
     * Parse test steps from TestRail steps text.
     *
     * TestRail format supports:
     * - [STEP N] action text
     * - [VERIFY] expected result text
     *
     * @param stepsText raw steps text from TestRail
     * @return list of parsed test case steps
     */
    private List<TestCaseStep> parseSteps(String stepsText) {
        if (isBlank(stepsText)) {
            log.debug("{} No steps text provided", LOG_PREFIX);
            return List.of();
        }

        // Remove HTML tags and clean up text
        String cleanText = stepsText
                .replaceAll("<[^>]+>", "\n")  // Replace HTML tags with newlines
                .replaceAll("&nbsp;", " ")     // Replace non-breaking spaces
                .replaceAll("\\s+", " ")       // Normalize whitespace
                .trim();

        log.debug("{} Parsing steps from text: {}", LOG_PREFIX, cleanText.substring(0, Math.min(100, cleanText.length())));

        List<TestCaseStep> steps = new ArrayList<>();

        // Extract [STEP N] blocks
        Matcher stepMatcher = STEP_PATTERN.matcher(cleanText);
        while (stepMatcher.find()) {
            String stepNumber = stepMatcher.group(1);
            String stepAction = stepMatcher.group(2).trim();

            log.debug("{} Found STEP {}: {}", LOG_PREFIX, stepNumber, stepAction.substring(0, Math.min(50, stepAction.length())));

            steps.add(new TestCaseStep(
                    stepAction,  // Just the action text without "Step N:" prefix
                    null,
                    null,
                    List.of()
            ));
        }

        // Extract [VERIFY] blocks
        Matcher verifyMatcher = VERIFY_PATTERN.matcher(cleanText);
        while (verifyMatcher.find()) {
            String expectedResult = verifyMatcher.group(1).trim();

            log.debug("{} Found VERIFY: {}", LOG_PREFIX, expectedResult.substring(0, Math.min(50, expectedResult.length())));

            steps.add(new TestCaseStep(
                    null,
                    expectedResult,  // Just the expected result without "Verify:" prefix
                    null,
                    List.of()
            ));
        }

        log.info("{} Parsed {} steps from TestRail format", LOG_PREFIX, steps.size());
        return steps;
    }

    /**
     * Build autotest mapping from TestRail custom fields.
     *
     * Extracts automation-related fields like test class, method, and scenario.
     *
     * @param custom TestRail custom fields
     * @return map of autotest properties
     */
    private Map<String, String> buildAutotestMapping(TestRailCustomFieldsXml custom) {
        if (custom == null) {
            return Map.of();
        }

        Map<String, String> mapping = new HashMap<>();

        if (isNotBlank(custom.getTestClass())) {
            mapping.put("testClass", custom.getTestClass());
        }

        if (isNotBlank(custom.getTestMethod())) {
            mapping.put("testMethod", custom.getTestMethod());
        }

        if (isNotBlank(custom.getScenario())) {
            mapping.put("scenario", custom.getScenario());
        }

        return mapping;
    }

    /**
     * Determine automation status based on TestRail custom fields.
     *
     * A test is considered automated if:
     * - automation_type field contains "automated", OR
     * - testClass field is present
     *
     * @param custom TestRail custom fields
     * @return automation status
     */
    private TestCaseAutomationStatus determineAutomationStatus(TestRailCustomFieldsXml custom) {
        if (custom == null) {
            return TestCaseAutomationStatus.NOT_AUTOMATED;
        }

        // Check automation_type field
        if (custom.getAutomationType() != null) {
            String automationType = trimToEmpty(custom.getAutomationType().getValue()).toLowerCase();
            if (automationType.contains("automated")) {
                return TestCaseAutomationStatus.AUTOMATED;
            }
        }

        // Check if test class is specified (indicates automation)
        if (isNotBlank(custom.getTestClass())) {
            return TestCaseAutomationStatus.AUTOMATED;
        }

        return TestCaseAutomationStatus.NOT_AUTOMATED;
    }

    /**
     * Parse tags from TestRail references field.
     *
     * References are typically comma or semicolon separated issue keys.
     * Example: "AUTO-517, AUTO-518"
     *
     * @param references references string from TestRail
     * @return list of tags (max 50 tags, max 50 chars each)
     */
    private List<String> parseTags(String references) {
        if (isBlank(references)) {
            return List.of();
        }

        return Arrays.stream(references.split("[,;\\s]+"))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .filter(tag -> tag.length() <= 50)
                .limit(50)
                .toList();
    }

    /**
     * Map TestRail test type to internal type name.
     *
     * Supported types: Functional, Smoke, Regression, Security, Performance, Usability
     * Default: Functional
     *
     * @param testRailType type from TestRail
     * @return internal type name
     */
    private String mapTypeToInternal(String testRailType) {
        if (isBlank(testRailType)) {
            return "Functional";
        }

        return switch (testRailType.trim().toLowerCase()) {
            case "functional" -> "Functional";
            case "smoke" -> "Smoke";
            case "regression" -> "Regression";
            case "security" -> "Security";
            case "performance" -> "Performance";
            case "usability" -> "Usability";
            case "other" -> "Functional";
            default -> "Functional";
        };
    }

    /**
     * Map TestRail priority to internal priority name.
     *
     * Supported priorities: Low, Medium, High, Critical
     * Default: Medium
     *
     * @param testRailPriority priority from TestRail
     * @return internal priority name
     */
    private String mapPriorityToInternal(String testRailPriority) {
        if (isBlank(testRailPriority)) {
            return "Medium";
        }

        return switch (testRailPriority.trim().toLowerCase()) {
            case "low" -> "Low";
            case "medium" -> "Medium";
            case "high" -> "High";
            case "critical" -> "Critical";
            default -> "Medium";
        };
    }
}
