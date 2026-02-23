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
    public SuiteResponse create(Long projectId, SuiteCreateRequest req) {
        log.info("{} create: projectId={}", LOG_PREFIX, projectId);

        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        String name = normalize(req.name());
        if (name.isBlank()) {
            throw new IllegalArgumentException("Suite name must not be blank");
        }
        ensureUniqueName(projectId, name);

        Suite suite = Suite.builder()
                .projectId(projectId)
                .name(name)
                .description(req.description())
                .archived(false)
                .build();

        Suite saved = suiteRepository.save(suite);

        log.info("{} created: suiteId={}, projectId={}", LOG_PREFIX, saved.getId(), projectId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SuiteResponse> listByProject(Long projectId) {
        log.info("{} list: projectId={}", LOG_PREFIX, projectId);

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
    public SuiteResponse get(Long id) {
        log.info("{} get: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);
        return toResponse(suite);
    }


    @Transactional
    public SuiteResponse update(Long id, SuiteUpdateRequest request) {
        log.info("{} update: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);

        if (request.name() != null) {
            String newName = normalize(request.name());
            if (!newName.isBlank() && !newName.equalsIgnoreCase(suite.getName())) {
                ensureUniqueName(suite.getProjectId(), newName);
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
    public void archive(Long id) {
        log.info("{} archive: suiteId={}", LOG_PREFIX, id);

        Suite suite = mustActiveSuite(id);

        Instant now = Instant.now();
        suite.setArchived(true);
        suite.setArchivedAt(now);
        suite.setUpdatedAt(now);

        // Remove run-case links for all active cases in this suite
        List<Long> caseIds = testCaseRepository
                .findAllActiveBySuiteId(suite.getProjectId(), suite.getId())
                .stream()
                .map(TestCase::getId)
                .toList();

        if (!caseIds.isEmpty()) {
            runCaseRepository.deleteByCaseIdIn(caseIds);
        }
    }

    /* ================== HELPERS ================== */

    private Suite mustActiveSuite(Long id) {
        return suiteRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Suite not found"));
    }

    private void ensureUniqueName(Long projectId, String name) {
        boolean exists = suiteRepository
                .existsActiveByProjectIdAndNameIgnoreCase(projectId, name);
        if (exists) {
            throw new IllegalArgumentException("Suite name already exists in this project");
        }
    }

    private SuiteResponse toResponse(Suite s) {
        return new SuiteResponse(
                s.getId(),
                s.getProjectId(),
                s.getName(),
                s.getDescription(),
                s.isArchived()
        );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
