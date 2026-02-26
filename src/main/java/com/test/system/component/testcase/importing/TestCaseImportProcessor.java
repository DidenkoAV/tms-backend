package com.test.system.component.testcase.importing;

import com.test.system.component.testcase.mapper.TestCaseMapper;
import com.test.system.dto.testcase.importexport.HierarchicalSuiteImportDto;
import com.test.system.dto.testcase.importexport.ImportContext;
import com.test.system.dto.testcase.importexport.ImportStats;
import com.test.system.dto.testcase.importexport.SuiteImport;
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
     * Supports both flat and hierarchical suite structures.
     *
     * @param projectId the project ID
     * @param importedSuites the list of suites to import
     * @param suiteByName the map of existing suite names to IDs (will be updated)
     */
    public void importSuites(
            Long projectId,
            List<SuiteImport> importedSuites,
            Map<String, Long> suiteByName
    ) {
        if (importedSuites == null || importedSuites.isEmpty()) {
            return;
        }

        log.info("{} ========== STARTING SUITE IMPORT ==========", LOG_PREFIX);
        log.info("{} Total suites to import: {}", LOG_PREFIX, importedSuites.size());

        // Separate hierarchical and flat suites
        List<HierarchicalSuiteImportDto> hierarchicalSuites = new ArrayList<>();
        List<SuiteImportDto> flatSuites = new ArrayList<>();

        for (SuiteImport suiteDto : importedSuites) {
            if (suiteDto instanceof HierarchicalSuiteImportDto h) {
                hierarchicalSuites.add(h);
                log.debug("{} Found hierarchical suite: name={}, parentName={}",
                        LOG_PREFIX, h.name(), h.parentName());
            } else if (suiteDto instanceof SuiteImportDto s) {
                flatSuites.add(s);
                log.debug("{} Found flat suite: name={}", LOG_PREFIX, s.name());
            }
        }

        log.info("{} Hierarchical suites: {}, Flat suites: {}",
                LOG_PREFIX, hierarchicalSuites.size(), flatSuites.size());

        // Import hierarchical suites first (respecting parent-child relationships)
        importHierarchicalSuites(projectId, hierarchicalSuites, suiteByName);

        // Import flat suites
        for (SuiteImportDto suiteDto : flatSuites) {
            String suiteName = trimToEmpty(suiteDto.name());

            if (isBlank(suiteName)) {
                continue;
            }

            String key = normalizeKey(suiteName);
            if (!suiteByName.containsKey(key)) {
                Suite suite = createSuite(projectId, null, 0, suiteName, suiteDto.description());
                suiteByName.put(key, suite.getId());
                log.debug("{} Created flat suite: {} (id={})", LOG_PREFIX, suiteName, suite.getId());
            }
        }
    }

    /**
     * Import hierarchical suites, creating parents before children.
     *
     * @param projectId the project ID
     * @param hierarchicalSuites the list of hierarchical suites
     * @param suiteByName the map of suite names to IDs
     */
    private void importHierarchicalSuites(
            Long projectId,
            List<HierarchicalSuiteImportDto> hierarchicalSuites,
            Map<String, Long> suiteByName
    ) {
        log.info("{} ========== HIERARCHICAL SUITE IMPORT START ==========", LOG_PREFIX);
        log.info("{} Total hierarchical suites to import: {}", LOG_PREFIX, hierarchicalSuites.size());

        // Build dependency graph
        Map<String, List<HierarchicalSuiteImportDto>> childrenByParent = new HashMap<>();
        List<HierarchicalSuiteImportDto> rootSuites = new ArrayList<>();

        for (HierarchicalSuiteImportDto suite : hierarchicalSuites) {
            log.debug("{} Processing suite: name='{}', parentName='{}'",
                    LOG_PREFIX, suite.name(), suite.parentName());

            if (suite.parentName() == null) {
                rootSuites.add(suite);
                log.debug("{} Added as ROOT suite: '{}'", LOG_PREFIX, suite.name());
            } else {
                String parentKey = normalizeKey(suite.parentName());
                childrenByParent
                        .computeIfAbsent(parentKey, k -> new ArrayList<>())
                        .add(suite);
                log.debug("{} Added as CHILD of '{}' (key='{}'): '{}'",
                        LOG_PREFIX, suite.parentName(), parentKey, suite.name());
            }
        }

        log.info("{} Root suites: {}", LOG_PREFIX, rootSuites.size());
        log.info("{} Parent keys in childrenByParent: {}", LOG_PREFIX, childrenByParent.keySet());

        // Process root suites first, then children recursively
        for (HierarchicalSuiteImportDto rootSuite : rootSuites) {
            log.info("{} Processing root suite: '{}'", LOG_PREFIX, rootSuite.name());
            importHierarchicalSuiteRecursive(
                    projectId,
                    rootSuite,
                    null,
                    0,
                    null,  // currentPath for root is null
                    childrenByParent,
                    suiteByName
            );
        }

        log.info("{} ========== HIERARCHICAL SUITE IMPORT COMPLETE ==========", LOG_PREFIX);
    }

    /**
     * Recursively import a suite and its children.
     *
     * @param projectId the project ID
     * @param suiteDto the suite to import
     * @param parentId the parent suite ID (null for root)
     * @param depth the nesting depth
     * @param currentPath the full path to current suite (null for root), e.g., "ui/sync"
     * @param childrenByParent map of children by parent path
     * @param suiteByName map of suite names to IDs
     */
    private void importHierarchicalSuiteRecursive(
            Long projectId,
            HierarchicalSuiteImportDto suiteDto,
            Long parentId,
            int depth,
            String currentPath,
            Map<String, List<HierarchicalSuiteImportDto>> childrenByParent,
            Map<String, Long> suiteByName
    ) {
        String suiteName = trimToEmpty(suiteDto.name());
        if (isBlank(suiteName)) {
            return;
        }

        log.debug("{} [importHierarchicalSuiteRecursive] CALLED: name={}, parentId={}, depth={}",
                LOG_PREFIX, suiteName, parentId, depth);

        // Check max depth
        if (depth > 4) {
            log.warn("{} Skipping suite {} - max depth (5) exceeded", LOG_PREFIX, suiteName);
            return;
        }

        // Build full path for current suite (text-based path like "ui/umh/chrome")
        String fullPath = currentPath != null
                ? currentPath + "/" + suiteName
                : suiteName;

        // Use full path as key to avoid conflicts between suites with same name but different parents
        String fullKey = buildSuiteKey(parentId, suiteName, suiteByName);
        String fullPathKey = normalizeKey(fullPath);
        String simpleKey = normalizeKey(suiteName);
        Long suiteId;

        if (!suiteByName.containsKey(fullKey)) {
            log.debug("{} [importHierarchicalSuiteRecursive] Creating suite: name={}, parentId={}, depth={}",
                    LOG_PREFIX, suiteName, parentId, depth);
            Suite suite = createSuite(projectId, parentId, depth, suiteName, suiteDto.description());
            suiteId = suite.getId();

            // Store with full key (always unique) - uses parentId/name format
            suiteByName.put(fullKey, suiteId);

            // Store with full path key - this allows test cases to find suites by full path
            suiteByName.put(fullPathKey, suiteId);
            log.debug("{} Registered suite with full path key: {} -> {}", LOG_PREFIX, fullPathKey, suiteId);

            // Also store with simple key if not already taken
            // This allows test cases to find suites by simple name when there's no ambiguity
            if (!suiteByName.containsKey(simpleKey)) {
                suiteByName.put(simpleKey, suiteId);
                log.debug("{} Registered suite with simple key: {} -> {}", LOG_PREFIX, simpleKey, suiteId);
            } else {
                log.debug("{} Simple key {} already taken, only using full keys ({}, {})",
                        LOG_PREFIX, simpleKey, fullKey, fullPathKey);
            }

            log.debug("{} Created hierarchical suite: {} (id={}, parent={}, depth={})",
                    LOG_PREFIX, suiteName, suiteId, parentId, depth);
        } else {
            suiteId = suiteByName.get(fullKey);
            log.debug("{} [importHierarchicalSuiteRecursive] Suite already exists: name={}, id={}",
                    LOG_PREFIX, suiteName, suiteId);
        }

        log.debug("{} Current suite full path: '{}'", LOG_PREFIX, fullPath);

        // Process children - use full path for lookup to distinguish between suites with same name
        // For example: children of "ui/sync/chrome" vs children of "ui/umh/chrome"
        List<HierarchicalSuiteImportDto> children = childrenByParent.get(fullPathKey);

        log.debug("{} Looking for children of '{}' using key '{}': found {} children",
                LOG_PREFIX, fullPath, fullPathKey, children != null ? children.size() : 0);

        if (children != null) {
            log.info("{} Processing {} children of suite '{}'", LOG_PREFIX, children.size(), fullPath);
            for (HierarchicalSuiteImportDto child : children) {
                log.debug("{} Processing child: '{}' of parent '{}'", LOG_PREFIX, child.name(), fullPath);
                importHierarchicalSuiteRecursive(
                        projectId,
                        child,
                        suiteId,
                        depth + 1,
                        fullPath,  // Pass full path to children
                        childrenByParent,
                        suiteByName
                );
            }
        } else {
            log.debug("{} No children found for suite '{}'", LOG_PREFIX, fullPath);
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

        // Create new suite (flat, at root level)
        Suite newSuite = createSuite(projectId, null, 0, suiteName, DEFAULT_SUITE_DESC);
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

    private Suite createSuite(Long projectId, Long parentId, Integer depth, String name, String description) {
        log.debug("{} [createSuite] BEFORE BUILD: name={}, parentId={}, depth={}",
                LOG_PREFIX, name, parentId, depth);

        Suite suite = Suite.builder()
                .projectId(projectId)
                .parentId(parentId)
                .depth(depth)
                .name(name)
                .description(isNotBlank(description) ? description : DEFAULT_SUITE_DESC)
                .archived(false)
                .build();

        log.debug("{} [createSuite] AFTER BUILD: id={}, name={}, parentId={}, depth={}",
                LOG_PREFIX, suite.getId(), suite.getName(), suite.getParentId(), suite.getDepth());

        Suite saved = suiteRepository.save(suite);

        log.debug("{} [createSuite] AFTER SAVE: id={}, name={}, parentId={}, depth={}",
                LOG_PREFIX, saved.getId(), saved.getName(), saved.getParentId(), saved.getDepth());

        return saved;
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

    /**
     * Build a unique key for a suite based on its parent and name.
     * This prevents collisions when multiple suites have the same name but different parents.
     *
     * @param parentId the parent suite ID (null for root suites)
     * @param suiteName the suite name
     * @param suiteByName the map of existing suite keys to IDs (not used currently but kept for future extensions)
     * @return a unique key for the suite
     */
    private String buildSuiteKey(Long parentId, String suiteName, Map<String, Long> suiteByName) {
        if (parentId == null) {
            return normalizeKey(suiteName);
        }
        return parentId + "/" + normalizeKey(suiteName);
    }
}

