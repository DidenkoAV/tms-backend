package com.test.system.component.testcase.testrail;

import com.test.system.dto.testcase.common.TestCaseStep;
import com.test.system.dto.testcase.importexport.SuiteImportDto;
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
     *
     * @param suiteXml the TestRail suite XML
     * @param projectId the target project ID
     * @return import request with suites and test cases
     */
    public TestCasesImportRequest convert(TestRailSuiteXml suiteXml, Long projectId) {
        log.info("{} Converting TestRail suite: {}", LOG_PREFIX, suiteXml.getName());

        List<SuiteImportDto> suites = new ArrayList<>();
        List<TestCaseResponse> cases = new ArrayList<>();

        // Process all sections recursively
        if (suiteXml.getSections() != null) {
            for (TestRailSectionXml section : suiteXml.getSections()) {
                processSection(section, "", suites, cases, projectId);
            }
        }

        log.info("{} Converted {} suites and {} test cases", LOG_PREFIX, suites.size(), cases.size());

        return new TestCasesImportRequest(projectId, suites, cases);
    }

    /**
     * Process a section recursively, building suite path and extracting test cases.
     *
     * @param section the section to process
     * @param parentPath the parent section path
     * @param suites the list to collect suites
     * @param cases the list to collect test cases
     * @param projectId the project ID
     */
    private void processSection(
            TestRailSectionXml section,
            String parentPath,
            List<SuiteImportDto> suites,
            List<TestCaseResponse> cases,
            Long projectId
    ) {
        String sectionName = trimToEmpty(section.getName());
        if (isBlank(sectionName)) {
            return;
        }

        // Build full path: parent/child
        String fullPath = isBlank(parentPath) ? sectionName : parentPath + " / " + sectionName;

        // Add suite if not already added
        boolean suiteExists = suites.stream()
                .anyMatch(s -> s.name().equalsIgnoreCase(fullPath));

        if (!suiteExists) {
            suites.add(new SuiteImportDto(fullPath, trimToEmpty(section.getDescription())));
            log.debug("{} Added suite: {}", LOG_PREFIX, fullPath);
        }

        // Process test cases in this section
        if (section.getCases() != null) {
            for (TestRailCaseXml caseXml : section.getCases()) {
                TestCaseResponse testCase = convertTestCase(caseXml, fullPath, projectId);
                if (testCase != null) {
                    cases.add(testCase);
                }
            }
        }

        // Process nested sections recursively
        if (section.getSections() != null) {
            for (TestRailSectionXml subsection : section.getSections()) {
                processSection(subsection, fullPath, suites, cases, projectId);
            }
        }
    }

    /**
     * Convert a single TestRail test case to internal format.
     *
     * @param caseXml the TestRail case XML
     * @param suiteName the suite name (section path)
     * @param projectId the project ID
     * @return converted test case response
     */
    private TestCaseResponse convertTestCase(TestRailCaseXml caseXml, String suiteName, Long projectId) {
        String title = trimToEmpty(caseXml.getTitle());
        if (isBlank(title)) {
            log.warn("{} Skipping test case with empty title", LOG_PREFIX);
            return null;
        }

        // Parse custom fields
        TestRailCustomFieldsXml custom = caseXml.getCustom();
        String preconditions = custom != null ? trimToNull(custom.getPreconditions()) : null;
        String stepsText = custom != null ? trimToNull(custom.getSteps()) : null;

        // Parse steps from text
        List<TestCaseStep> steps = parseSteps(stepsText);

        // Build autotest mapping
        Map<String, String> autotestMapping = buildAutotestMapping(custom);

        // Determine automation status
        TestCaseAutomationStatus automationStatus = determineAutomationStatus(custom);

        // Parse tags from references
        List<String> tags = parseTags(caseXml.getReferences());

        return new TestCaseResponse(
                null,                           // id (will be generated)
                projectId,                      // projectId
                null,                           // suiteId (will be resolved)
                suiteName,                      // suiteName
                title,                          // title
                null,                           // typeId (will be resolved from type name)
                mapType(caseXml.getType()),    // typeName
                null,                           // priorityId (will be resolved from priority name)
                mapPriority(caseXml.getPriority()), // priorityName
                0,                              // estimateSeconds
                preconditions,                  // preconditions
                0,                              // sortIndex
                false,                          // archived
                null,                           // expectedResult
                null,                           // actualResult
                null,                           // testData
                steps,                          // steps
                List.of(),                      // attachments
                TestCaseStatus.DRAFT,           // status
                TestCaseSeverity.NORMAL,        // severity
                automationStatus,               // automationStatus
                tags,                           // tags
                autotestMapping,                // autotestMapping
                Instant.now(),                  // createdAt
                Instant.now(),                  // updatedAt
                null,                           // createdBy
                null,                           // createdByName
                null                            // createdByEmail
        );
    }

    /**
     * Parse steps from TestRail steps text.
     * Supports [STEP N] and [VERIFY] markers.
     *
     * @param stepsText the steps text
     * @return list of test case steps
     */
    private List<TestCaseStep> parseSteps(String stepsText) {
        if (isBlank(stepsText)) {
            return List.of();
        }

        List<TestCaseStep> steps = new ArrayList<>();

        // Remove HTML tags if present
        String cleanText = stepsText.replaceAll("<[^>]+>", "\n").trim();

        // Find all [STEP N] blocks
        Matcher stepMatcher = STEP_PATTERN.matcher(cleanText);
        while (stepMatcher.find()) {
            String stepNumber = stepMatcher.group(1);
            String stepText = stepMatcher.group(2).trim();

            steps.add(new TestCaseStep(
                    "Step " + stepNumber + ": " + stepText,
                    null,
                    null,
                    List.of()
            ));
        }

        // Find all [VERIFY] blocks and add as separate steps
        Matcher verifyMatcher = VERIFY_PATTERN.matcher(cleanText);
        while (verifyMatcher.find()) {
            String verifyText = verifyMatcher.group(1).trim();

            steps.add(new TestCaseStep(
                    null,
                    "Verify: " + verifyText,
                    null,
                    List.of()
            ));
        }

        return steps;
    }

    /**
     * Build autotest mapping from custom fields.
     *
     * @param custom the custom fields
     * @return autotest mapping
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
     * Determine automation status from custom fields.
     *
     * @param custom the custom fields
     * @return automation status
     */
    private TestCaseAutomationStatus determineAutomationStatus(TestRailCustomFieldsXml custom) {
        if (custom == null || custom.getAutomationType() == null) {
            return TestCaseAutomationStatus.NOT_AUTOMATED;
        }

        String value = trimToEmpty(custom.getAutomationType().getValue()).toLowerCase();

        if (value.contains("automated") || isNotBlank(custom.getTestClass())) {
            return TestCaseAutomationStatus.AUTOMATED;
        }

        return TestCaseAutomationStatus.NOT_AUTOMATED;
    }

    /**
     * Parse tags from references field.
     *
     * @param references the references string (e.g., "AUTO-517, AUTO-518")
     * @return list of tags
     */
    private List<String> parseTags(String references) {
        if (isBlank(references)) {
            return List.of();
        }

        return Arrays.stream(references.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> s.length() <= 50)
                .limit(50)
                .toList();
    }

    /**
     * Map TestRail type to internal type name.
     *
     * @param type the TestRail type
     * @return internal type name
     */
    private String mapType(String type) {
        if (isBlank(type)) {
            return "Functional";
        }

        String normalized = type.trim().toLowerCase();

        return switch (normalized) {
            case "functional" -> "Functional";
            case "smoke" -> "Smoke";
            case "regression" -> "Regression";
            case "security" -> "Security";
            case "performance" -> "Performance";
            case "usability" -> "Usability";
            case "other" -> "Functional"; // Map "Other" to "Functional"
            default -> "Functional";
        };
    }

    /**
     * Map TestRail priority to internal priority name.
     *
     * @param priority the TestRail priority
     * @return internal priority name
     */
    private String mapPriority(String priority) {
        if (isBlank(priority)) {
            return "Medium";
        }

        String normalized = priority.trim().toLowerCase();

        return switch (normalized) {
            case "low" -> "Low";
            case "medium" -> "Medium";
            case "high" -> "High";
            case "critical" -> "Critical";
            default -> "Medium";
        };
    }
}
