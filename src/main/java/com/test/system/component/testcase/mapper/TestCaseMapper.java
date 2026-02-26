package com.test.system.component.testcase.mapper;

import com.test.system.dto.testcase.common.TestCaseAttachment;
import com.test.system.dto.testcase.common.TestCaseStep;
import com.test.system.dto.testcase.mapper.LookupMaps;
import com.test.system.dto.testcase.request.CreateTestCaseRequest;
import com.test.system.dto.testcase.request.UpdateTestCaseRequest;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.enums.testcase.TestCaseAutomationStatus;
import com.test.system.enums.testcase.TestCaseSeverity;
import com.test.system.enums.testcase.TestCaseStatus;
import com.test.system.model.cases.TestCase;
import com.test.system.model.user.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.test.system.utils.StringNormalizer.safe;
import static com.test.system.utils.StringNormalizer.trimToEmpty;

/**
 * Pure mapper for converting between TestCase entities and DTOs.
 * Does NOT access database - all lookup data must be provided via LookupMaps.
 * This avoids N+1 query problems.
 */
@Component
public class TestCaseMapper {

    /* ============================================================
       Entity to DTO
       ============================================================ */

    /**
     * Convert TestCase entity to TestCaseResponse DTO.
     * All lookup data must be provided via lookupMaps to avoid N+1 queries.
     *
     * @param testCase the test case entity
     * @param lookupMaps pre-loaded lookup data (suites, types, priorities, authors)
     * @return test case response DTO
     */
    public TestCaseResponse toResponse(TestCase testCase, LookupMaps lookupMaps) {
        User author = lookupMaps.getAuthor(testCase.getCreatedBy());
        User assignee = lookupMaps.getAuthor(testCase.getAssignedTo());

        return new TestCaseResponse(
                testCase.getId(),
                testCase.getProjectId(),
                testCase.getSuiteId(),
                lookupMaps.getSuiteName(testCase.getSuiteId()),
                testCase.getTitle(),
                testCase.getTypeId(),
                lookupMaps.getTypeName(testCase.getTypeId()),
                testCase.getPriorityId(),
                lookupMaps.getPriorityName(testCase.getPriorityId()),
                testCase.getEstimateSeconds(),
                testCase.getPreconditions(),
                testCase.getSortIndex(),
                testCase.isArchived(),
                testCase.getExpectedResult(),
                testCase.getActualResult(),
                testCase.getTestData(),
                mapStepsToDto(testCase.getSteps()),
                mapAttachmentsToDto(testCase.getAttachments()),
                mapEnum(testCase.getStatus(), TestCaseStatus::valueOf),
                mapEnum(testCase.getSeverity(), TestCaseSeverity::valueOf),
                mapEnum(testCase.getAutomationStatus(), TestCaseAutomationStatus::valueOf),
                mapTagsToDto(testCase.getTags()),
                testCase.getAutotestMapping(),
                testCase.getCreatedAt(),
                testCase.getUpdatedAt(),
                testCase.getCreatedBy(),
                author != null ? author.getFullName() : null,
                author != null ? author.getEmail() : null,
                testCase.getAssignedTo(),
                assignee != null ? assignee.getFullName() : null,
                assignee != null ? assignee.getEmail() : null
        );
    }

    /* ============================================================
       DTO to Entity
       ============================================================ */

    /**
     * Build new TestCase entity from CreateTestCaseRequest.
     * Time must be provided externally for testability and consistency.
     *
     * @param request the create request
     * @param projectId the project ID
     * @param author the author user
     * @param now current timestamp
     * @return new test case entity
     */
    public TestCase buildNewEntity(CreateTestCaseRequest request, Long projectId, User author, Instant now) {
        return TestCase.builder()
                .projectId(projectId)
                .suiteId(request.suiteId())
                .title(trimToEmpty(request.title()))
                .typeId(request.typeId())
                .priorityId(request.priorityId())
                .assignedTo(request.assignedTo())
                .estimateSeconds(request.estimateSeconds() != null ? request.estimateSeconds() : 0)
                .preconditions(request.preconditions())
                .sortIndex(Optional.ofNullable(request.sortIndex()).orElse(0))
                .expectedResult(request.expectedResult())
                .actualResult(request.actualResult())
                .testData(request.testData())
                .steps(mapStepsFromDto(request.steps()))
                .attachments(mapAttachmentsFromDto(request.attachments()))
                .status(mapDtoEnum(request.status(), TestCase.Status::valueOf, TestCase.Status.DRAFT))
                .severity(mapDtoEnum(request.severity(), TestCase.Severity::valueOf, TestCase.Severity.NORMAL))
                .automationStatus(mapDtoEnum(request.automationStatus(),
                        TestCase.AutomationStatus::valueOf, TestCase.AutomationStatus.NOT_AUTOMATED))
                .tags(request.tags() != null ? request.tags().toArray(new String[0]) : new String[0])
                .autotestMapping(Optional.ofNullable(request.autotestMapping()).orElseGet(HashMap::new))
                .createdBy(author.getId())
                .createdAt(now)
                .updatedAt(now)
                .archived(false)
                .build();
    }

    /**
     * Update existing TestCase entity from UpdateTestCaseRequest.
     * Time must be provided externally for testability and consistency.
     *
     * @param existing the existing test case entity
     * @param request the update request
     * @param now current timestamp
     */
    public void updateFromRequest(TestCase existing, UpdateTestCaseRequest request, Instant now) {
        existing.setPriorityId(request.priorityId());
        existing.setTypeId(request.typeId());
        existing.setAssignedTo(request.assignedTo());
        existing.setEstimateSeconds(defaultInt(request.estimateSeconds()));
        existing.setSortIndex(defaultInt(request.sortIndex()));
        existing.setPreconditions(request.preconditions());
        existing.setExpectedResult(request.expectedResult());
        existing.setActualResult(request.actualResult());
        existing.setTestData(request.testData());

        // Collections - only update if provided
        applyIfNotNull(request.steps(), v -> existing.setSteps(mapStepsFromDto(v)));
        applyIfNotNull(request.attachments(), v -> existing.setAttachments(mapAttachmentsFromDto(v)));
        applyIfNotNull(request.tags(), v -> existing.setTags(v.toArray(new String[0])));
        applyIfNotNull(request.autotestMapping(), existing::setAutotestMapping);

        // Enums - only update if provided
        applyIfNotNull(request.status(), v -> existing.setStatus(TestCase.Status.valueOf(v.name())));
        applyIfNotNull(request.severity(), v -> existing.setSeverity(TestCase.Severity.valueOf(v.name())));
        applyIfNotNull(request.automationStatus(),
                v -> existing.setAutomationStatus(TestCase.AutomationStatus.valueOf(v.name())));

        existing.setUpdatedAt(now);
    }

    /**
     * Update existing TestCase entity from TestCaseResponse DTO.
     * Time must be provided externally for testability and consistency.
     *
     * @param existing the existing test case entity
     * @param dto the update data
     * @param now current timestamp
     */
    public void updateFromDto(TestCase existing, TestCaseResponse dto, Instant now) {
        existing.setTypeId(dto.typeId());
        existing.setPriorityId(dto.priorityId());
        existing.setAssignedTo(dto.assignedTo());
        existing.setEstimateSeconds(dto.estimateSeconds());
        existing.setPreconditions(dto.preconditions());
        existing.setExpectedResult(dto.expectedResult());
        existing.setActualResult(dto.actualResult());
        existing.setTestData(dto.testData());
        existing.setSteps(mapStepsFromDto(dto.steps()));
        existing.setAttachments(mapAttachmentsFromDto(dto.attachments()));
        existing.setTags(dto.tags() != null ? dto.tags().toArray(new String[0]) : new String[0]);
        existing.setAutotestMapping(dto.autotestMapping() != null ? dto.autotestMapping() : new HashMap<>());

        if (dto.status() != null) {
            existing.setStatus(TestCase.Status.valueOf(dto.status().name()));
        }
        if (dto.severity() != null) {
            existing.setSeverity(TestCase.Severity.valueOf(dto.severity().name()));
        }

        if (dto.automationStatus() != null) {
            existing.setAutomationStatus(TestCase.AutomationStatus.valueOf(dto.automationStatus().name()));
        }

        existing.setUpdatedAt(now);
    }

    /* ============================================================
       Steps mapping
       ============================================================ */

    public List<TestCase.Step> mapStepsFromDto(List<TestCaseStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        return steps.stream()
                .map(s -> TestCase.Step.builder()
                        .action(s.action())
                        .expected(s.expected())
                        .notes(s.notes())
                        .attachments(mapAttachmentsFromDto(s.attachments()))
                        .build())
                .toList();
    }

    public List<TestCaseStep> mapStepsToDto(List<TestCase.Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        return steps.stream()
                .map(s -> new TestCaseStep(
                        s.getAction(),
                        s.getExpected(),
                        s.getNotes(),
                        mapAttachmentsToDto(s.getAttachments())
                ))
                .toList();
    }

    /* ============================================================
       Attachments mapping
       ============================================================ */

    public List<TestCase.Attachment> mapAttachmentsFromDto(List<TestCaseAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(a -> new TestCase.Attachment(a.name(), a.url(), a.size(), a.mime()))
                .toList();
    }

    public List<TestCaseAttachment> mapAttachmentsToDto(List<TestCase.Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(a -> new TestCaseAttachment(a.getName(), a.getUrl(), a.getSize(), a.getMime()))
                .toList();
    }

    /* ============================================================
       Enum mapping
       ============================================================ */

    private <E extends Enum<E>, D extends Enum<D>> D mapEnum(
            E source,
            java.util.function.Function<String, D> mapper
    ) {
        return source == null ? null : mapper.apply(source.name());
    }

    private <D extends Enum<D>, E extends Enum<E>> E mapDtoEnum(
            D source,
            java.util.function.Function<String, E> mapper,
            E defaultValue
    ) {
        return source == null ? defaultValue : mapper.apply(source.name());
    }

    /* ============================================================
       Tags mapping
       ============================================================ */

    /**
     * Map tags array to immutable list.
     * Normalizes null to empty list.
     */
    private List<String> mapTagsToDto(String[] tags) {
        if (tags == null || tags.length == 0) {
            return List.of();
        }
        return List.of(tags);
    }

    /* ============================================================
       Helper methods
       ============================================================ */

    private static int defaultInt(Integer value) {
        return value != null ? value : 0;
    }

    private static <T> void applyIfNotNull(T value, java.util.function.Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
