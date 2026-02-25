package com.test.system.service.suite;

import com.test.system.dto.suite.SuiteCreateRequest;
import com.test.system.dto.suite.SuiteResponse;
import com.test.system.dto.suite.SuiteUpdateRequest;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.cases.TestCase;
import com.test.system.model.suite.Suite;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.run.TestRunCaseRepository;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuiteService {

    private static final String LOG_PREFIX = "[Suite]";

    private final TestSuiteRepository suiteRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunCaseRepository runCaseRepository;

    @Transactional
    public SuiteResponse createSuite(Long projectId, SuiteCreateRequest req) {
        log.info("{} createSuite: projectId={}, parentId={}", LOG_PREFIX, projectId, req.parentId());

        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        String name = normalize(req.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Suite name must not be blank");
        }

        // Validate parent suite and calculate depth
        Integer depth = 0;
        if (req.parentId() != null) {
            Suite parent = mustActiveSuite(req.parentId());

            // Validate parent belongs to same project
            if (!parent.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("Parent suite must belong to the same project");
            }

            // Check max depth (0-4, so parent can be at most depth 3)
            if (parent.getDepth() >= 4) {
                throw new IllegalArgumentException("Maximum nesting depth (5 levels) exceeded");
            }

            depth = parent.getDepth() + 1;
        }

        ensureUniqueName(projectId, req.parentId(), name);

        Suite suite = Suite.builder()
                .projectId(projectId)
                .parentId(req.parentId())
                .depth(depth)
                .name(name)
                .description(req.description())
                .archived(false)
                .build();

        Suite saved = suiteRepository.save(suite);

        log.info("{} created: suiteId={}, projectId={}, parentId={}, depth={}",
                LOG_PREFIX, saved.getId(), projectId, req.parentId(), depth);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SuiteResponse> listSuitesByProject(Long projectId) {
        log.info("{} listSuitesByProject: projectId={}", LOG_PREFIX, projectId);

        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        List<Suite> suites = suiteRepository
                .findAllActiveByProjectId(projectId);

        log.info("{} list: projectId={}, count={}", LOG_PREFIX, projectId, suites.size());
        return suites.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SuiteResponse getSuite(Long id) {
        log.info("{} getSuite: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);
        return toResponse(suite);
    }


    @Transactional
    public SuiteResponse updateSuite(Long id, SuiteUpdateRequest request) {
        log.info("{} updateSuite: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);

        if (request.name() != null) {
            String newName = normalize(request.name());
            if (!newName.isBlank() && !newName.equalsIgnoreCase(suite.getName())) {
                ensureUniqueName(suite.getProjectId(), suite.getParentId(), newName);
                suite.setName(newName);
            }
        }

        if (request.description() != null) {
            suite.setDescription(request.description());
        }

        suite.setUpdatedAt(Instant.now());
        Suite saved = suiteRepository.save(suite);

        return toResponse(saved);
    }


    @Transactional
    public void deleteSuite(Long id) {
        log.info("{} archiveSuite: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);

        // Archive this suite and all child suites recursively
        archiveSuiteRecursive(suite);
    }

    /**
     * Archive multiple suites in a single transaction (batch operation).
     * More efficient than calling archiveSuite() multiple times.
     * Automatically includes all child suites.
     *
     * @param suiteIds list of suite IDs to archive
     * @return number of suites archived (including children)
     */
    @Transactional
    public int deleteSuitesBatch(List<Long> suiteIds) {
        if (suiteIds == null || suiteIds.isEmpty()) {
            log.warn("{} archiveSuitesBatch: empty suite list", LOG_PREFIX);
            return 0;
        }

        log.info("{} archiveSuitesBatch: count={}, ids={}", LOG_PREFIX, suiteIds.size(), suiteIds);

        // 1. Load all requested suites
        List<Suite> requestedSuites = suiteRepository.findAllActiveByIdIn(suiteIds);
        if (requestedSuites.isEmpty()) {
            log.warn("{} archiveSuitesBatch: no active suites found", LOG_PREFIX);
            return 0;
        }

        // 2. Collect all suites to archive (including children recursively)
        List<Suite> allSuitesToArchive = collectAllSuitesRecursive(requestedSuites);

        // 3. Collect all case IDs from all suites
        List<Long> allSuiteIds = allSuitesToArchive.stream()
                .map(Suite::getId)
                .toList();

        List<Long> allCaseIds = testCaseRepository
                .findAllActiveBySuiteIdIn(allSuiteIds)
                .stream()
                .map(TestCase::getId)
                .toList();

        // 4. Delete run-case links in batch
        if (!allCaseIds.isEmpty()) {
            log.info("{} archiveSuitesBatch: deleting run-case links for {} cases", LOG_PREFIX, allCaseIds.size());
            runCaseRepository.deleteByCaseIdIn(allCaseIds);
        }

        // 5. Archive all suites in batch
        Instant now = Instant.now();
        for (Suite suite : allSuitesToArchive) {
            suite.setArchived(true);
            suite.setArchivedAt(now);
            suite.setUpdatedAt(now);
        }
        suiteRepository.saveAll(allSuitesToArchive);

        int archivedCount = allSuitesToArchive.size();
        log.info("{} archiveSuitesBatch: archived {} suites (including children)", LOG_PREFIX, archivedCount);

        return archivedCount;
    }

    private void archiveSuiteRecursive(Suite suite) {
        Instant now = Instant.now();
        suite.setArchived(true);
        suite.setArchivedAt(now);
        suite.setUpdatedAt(now);
        suiteRepository.save(suite);

        // Remove run-case links for all active cases in this suite
        List<Long> caseIds = testCaseRepository
                .findAllActiveBySuiteId(suite.getProjectId(), suite.getId())
                .stream()
                .map(TestCase::getId)
                .toList();

        if (!caseIds.isEmpty()) {
            runCaseRepository.deleteByCaseIdIn(caseIds);
        }

        // Archive all child suites
        List<Suite> children = suiteRepository.findAllActiveByParentId(suite.getId());
        for (Suite child : children) {
            archiveSuiteRecursive(child);
        }
    }

    /**
     * Collect all suites including their children recursively.
     * Uses iterative approach with batching to avoid N+1 queries.
     */
    private List<Suite> collectAllSuitesRecursive(List<Suite> rootSuites) {
        List<Suite> allSuites = new ArrayList<>(rootSuites);
        List<Suite> currentLevel = new ArrayList<>(rootSuites);

        // Iteratively collect children level by level
        while (!currentLevel.isEmpty()) {
            List<Long> parentIds = currentLevel.stream()
                    .map(Suite::getId)
                    .toList();

            List<Suite> children = suiteRepository.findAllActiveByParentIdIn(parentIds);
            if (children.isEmpty()) {
                break;
            }

            allSuites.addAll(children);
            currentLevel = children;
        }

        return allSuites;
    }

    /* ================== HELPERS ================== */

    private Suite mustActiveSuite(Long id) {
        return suiteRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Suite not found"));
    }

    private void ensureUniqueName(Long projectId, Long parentId, String name) {
        boolean exists = suiteRepository
                .existsActiveByProjectIdAndParentIdAndNameIgnoreCase(projectId, parentId, name);
        if (exists) {
            String location = parentId == null ? "at root level" : "under this parent suite";
            throw new IllegalArgumentException("Suite name already exists " + location);
        }
    }

    private SuiteResponse toResponse(Suite s) {
        return new SuiteResponse(
                s.getId(),
                s.getProjectId(),
                s.getParentId(),
                s.getDepth(),
                s.getName(),
                s.getDescription(),
                s.isArchived()
        );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
