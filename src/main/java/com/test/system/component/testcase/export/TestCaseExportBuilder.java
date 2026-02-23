package com.test.system.component.testcase.export;

import com.test.system.component.testcase.mapper.LookupDataResolver;
import com.test.system.component.testcase.mapper.TestCaseMapper;
import com.test.system.dto.testcase.importexport.SuiteExportDto;
import com.test.system.dto.testcase.importexport.TestCasesExportResponse;
import com.test.system.dto.testcase.mapper.LookupMaps;
import com.test.system.dto.testcase.response.TestCaseResponse;
import com.test.system.model.cases.TestCase;
import com.test.system.repository.suite.TestSuiteRepository;
import com.test.system.repository.testcase.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.test.system.utils.StringNormalizer.safe;

/**
 * Builder for creating test case export data.
 * Loads data from database and constructs export DTO.
 * Uses batch loading to avoid N+1 queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestCaseExportBuilder {

    private static final String LOG_PREFIX = "[ExportBuilder]";

    private final TestCaseRepository testCaseRepository;
    private final TestSuiteRepository suiteRepository;
    private final LookupDataResolver lookupResolver;
    private final TestCaseMapper mapper;

    /**
     * Build export data for a project.
     * Loads all active test cases and suites from the database.
     * Uses batch loading to avoid N+1 queries.
     *
     * @param projectId the project ID
     * @return export response with test cases and suites
     */
    public TestCasesExportResponse buildExportData(Long projectId) {
        log.debug("{} Building export data for projectId={}", LOG_PREFIX, projectId);

        // 1. Load test cases
        List<TestCase> testCases = testCaseRepository.findAllActiveByProjectId(projectId);
        log.debug("{} Loaded {} test cases", LOG_PREFIX, testCases.size());

        // 2. Batch load all lookup data (avoids N+1 queries)
        LookupMaps lookupMaps = lookupResolver.resolve(testCases);

        // 3. Convert test cases to DTOs (pure mapping, no DB access)
        List<TestCaseResponse> exportedCases = testCases.stream()
                .map(tc -> mapper.toResponse(tc, lookupMaps))
                .toList();

        // 4. Load and convert suites
        List<SuiteExportDto> exportedSuites = loadSuites(projectId);

        log.debug("{} Export data built: {} cases, {} suites",
                LOG_PREFIX, exportedCases.size(), exportedSuites.size());

        return new TestCasesExportResponse(
                projectId,
                Instant.now(),
                exportedCases.size(),
                exportedSuites,
                exportedCases
        );
    }

    /**
     * Load all active suites for a project.
     *
     * @param projectId the project ID
     * @return list of suite export DTOs
     */
    private List<SuiteExportDto> loadSuites(Long projectId) {
        return suiteRepository.findAllActiveByProjectId(projectId)
                .stream()
                .map(s -> new SuiteExportDto(
                        s.getId(),
                        s.getName(),
                        safe(s.getDescription())
                ))
                .toList();
    }
}

