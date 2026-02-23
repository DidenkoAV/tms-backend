package com.test.system.component.testcase.importing;

import com.test.system.component.testcase.mapper.TestCaseMapper;
import com.test.system.dto.testcase.importexport.ImportContext;
import com.test.system.dto.testcase.importexport.ImportStats;
import com.test.system.dto.testcase.importexport.SuiteImportDto;
import com.test.system.dto.testcase.request.CreateTestCaseRequest;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.model.cases.TestCase;
import com.test.system.model.suite.Suite;
import com.test.system.model.user.User;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCasePriorityRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import com.test.system.repository.testcase.TestCaseTypeRepository;
import com.test.system.provider.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.test.system.utils.StringNormalizer.*;

/**
 * Processor for importing test cases.
 * Handles suite creation, test case creation/update logic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestCaseImportProcessor {

    private static final String LOG_PREFIX = "[ImportProcessor]";
    private static final String DEFAULT_SUITE_DESC = "Imported suite";

    private final TestCaseRepository testCaseRepository;
    private final TestSuiteRepository suiteRepository;
    private final TestCasePriorityRepository priorityRepository;
    private final TestCaseTypeRepository caseTypeRepository;
    private final TestCaseMapper mapper;
    private final TimeProvider timeProvider;

    /**
     * Import suites into the project.
     * Creates suites that don't exist yet.
     *
     * @param projectId the project ID
     * @param importedSuites the list of suites to import
     * @param suiteByName the map of existing suite names to IDs (will be updated)
     */
    public void importSuites(
            Long projectId,
            List<SuiteImportDto> importedSuites,
            Map<String, Long> suiteByName
    ) {
        if (importedSuites == null || importedSuites.isEmpty()) {
            return;
        }

        for (SuiteImportDto suiteDto : importedSuites) {
            String suiteName = trimToEmpty(suiteDto.name());

            if (isBlank(suiteName)) {
                continue;
            }

            String key = normalizeKey(suiteName);
            if (!suiteByName.containsKey(key)) {
                Suite suite = createSuite(projectId, suiteName, suiteDto.description());
                suiteByName.put(key, suite.getId());
                log.debug("{} Created suite: {} (id={})", LOG_PREFIX, suiteName, suite.getId());
            }
        }
    }

    /**
     * Import test cases into the project.
     *
     * @param projectId the project ID
     * @param importedCases the list of test cases to import
     * @param overwriteExisting whether to overwrite existing test cases
     * @param author the user performing the import
     * @param context the import context with lookup data
     * @return import statistics
     */
    public ImportStats importTestCases(
            Long projectId,
            List<TestCaseResponse> importedCases,
            boolean overwriteExisting,
            User author,
            ImportContext context
    ) {
        if (importedCases == null || importedCases.isEmpty()) {
            return new ImportStats(0, 0, 0);
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;

        List<TestCase> newCases = new ArrayList<>();

        for (TestCaseResponse dto : importedCases) {
            // Step 1: Validate
            if (!validateCaseDto(dto)) {
                continue;
            }

            String cleanTitle = trimToEmpty(dto.title());

            // Step 2: Resolve suite
            Long suiteId = resolveSuiteId(projectId, dto, context.suiteByName());
            String suiteKey = String.valueOf(suiteId);

            // Step 3: Find existing
            TestCase existing = findExisting(context.existingCases(), suiteKey, cleanTitle);

            // Step 4: Apply update or create new
            if (existing != null) {
                if (overwriteExisting) {
                    applyUpdate(existing, dto);
                    testCaseRepository.save(existing);
                    updated++;
                } else {
                    skipped++;
                }
                continue;
            }

            // Step 5: Build new entity
            TestCase newCase = buildNew(dto, projectId, suiteId, author, context);
            newCases.add(newCase);

            // Update context for future lookups
            context.existingCases()
                    .computeIfAbsent(suiteKey, k -> new HashMap<>())
                    .put(normalizeKey(cleanTitle), newCase);

            created++;
        }

        // Batch save new cases
        if (!newCases.isEmpty()) {
            testCaseRepository.saveAll(newCases);
            log.debug("{} Saved {} new test cases", LOG_PREFIX, newCases.size());
        }

        return new ImportStats(created, skipped, updated);
    }

    /* ============================================================
       Import pipeline steps
       ============================================================ */

    /**
     * Step 1: Validate test case DTO.
     */
    private boolean validateCaseDto(TestCaseResponse dto) {
        return isNotBlank(dto.title());
    }

    /**
     * Step 2: Resolve suite ID for test case.
     * Creates suite if it doesn't exist.
     */
    private Long resolveSuiteId(Long projectId, TestCaseResponse dto, Map<String, Long> suiteByName) {
        if (isBlank(dto.suiteName())) {
            return null;
        }

        String suiteName = trimToEmpty(dto.suiteName());
        String key = normalizeKey(suiteName);

        Long suiteId = suiteByName.get(key);
        if (suiteId != null) {
            return suiteId;
        }

        // Create new suite
        Suite newSuite = createSuite(projectId, suiteName, DEFAULT_SUITE_DESC);
        suiteByName.put(key, newSuite.getId());
        log.debug("{} Created suite on-the-fly: {} (id={})", LOG_PREFIX, suiteName, newSuite.getId());

        return newSuite.getId();
    }

    /**
     * Step 3: Find existing test case.
     */
    private TestCase findExisting(
            Map<String, Map<String, TestCase>> existingCases,
            String suiteKey,
            String cleanTitle
    ) {
        return existingCases
                .getOrDefault(suiteKey, Collections.emptyMap())
                .get(normalizeKey(cleanTitle));
    }

    /**
     * Step 4a: Apply update to existing test case.
     */
    private void applyUpdate(TestCase existing, TestCaseResponse dto) {
        Instant now = timeProvider.now();
        mapper.updateFromDto(existing, dto, now);
    }

    /**
     * Step 4b: Build new test case entity.
     */
    private TestCase buildNew(
            TestCaseResponse dto,
            Long projectId,
            Long suiteId,
            User author,
            ImportContext context
    ) {
        Long priorityId = resolvePriorityId(dto, context.priorityByName());
        Long typeId = resolveTypeId(dto, context.typeByName());

        CreateTestCaseRequest request = new CreateTestCaseRequest(
                projectId,
                suiteId,
                dto.title(),
                typeId,
                priorityId,
                dto.estimateSeconds(),
                dto.preconditions(),
                dto.sortIndex(),
                dto.expectedResult(),
                dto.actualResult(),
                dto.testData(),
                dto.steps(),
                dto.attachments(),
                dto.status(),
                dto.severity(),
                dto.automationStatus(),
                dto.tags(),
                dto.autotestMapping()
        );

        Instant now = timeProvider.now();
        return mapper.buildNewEntity(request, projectId, author, now);
    }

    /* ============================================================
       Helper methods
       ============================================================ */

    private Suite createSuite(Long projectId, String name, String description) {
        Suite suite = Suite.builder()
                .projectId(projectId)
                .name(name)
                .description(isNotBlank(description) ? description : DEFAULT_SUITE_DESC)
                .archived(false)
                .build();
        return suiteRepository.save(suite);
    }

    private Long resolvePriorityId(TestCaseResponse dto, Map<String, Long> priorityByName) {
        Long priorityId = dto.priorityId();

        // If ID is invalid, try to resolve by name
        if ((priorityId == null || !priorityRepository.existsById(priorityId))
                && isNotBlank(dto.priorityName())) {
            priorityId = priorityByName.get(normalizeKey(dto.priorityName()));
        }

        return priorityId;
    }

    private Long resolveTypeId(TestCaseResponse dto, Map<String, Long> typeByName) {
        Long typeId = dto.typeId();

        // If ID is invalid, try to resolve by name
        if ((typeId == null || !caseTypeRepository.existsById(typeId))
                && isNotBlank(dto.typeName())) {
            typeId = typeByName.get(normalizeKey(dto.typeName()));
        }

        return typeId;
    }
}

