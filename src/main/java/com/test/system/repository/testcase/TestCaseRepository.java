package com.test.system.repository.testcase;

import com.test.system.model.cases.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    /**
     * Checks if a non-archived test case with the given title exists in the project (without suite, case-insensitive).
     *
     * @param projectId the ID of the project
     * @param title the test case title to check
     * @return true if a non-archived test case with this title exists (without suite), false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END FROM TestCase tc " +
           "WHERE tc.projectId = :projectId AND tc.suiteId IS NULL AND LOWER(tc.title) = LOWER(:title) AND tc.archived = false")
    boolean existsActiveByProjectIdAndNoSuiteAndTitleIgnoreCase(@Param("projectId") Long projectId, @Param("title") String title);

    /**
     * Checks if a non-archived test case with the given title exists in the suite (case-insensitive).
     *
     * @param projectId the ID of the project
     * @param suiteId the ID of the suite
     * @param title the test case title to check
     * @return true if a non-archived test case with this title exists in the suite, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END FROM TestCase tc " +
           "WHERE tc.projectId = :projectId AND tc.suiteId = :suiteId AND LOWER(tc.title) = LOWER(:title) AND tc.archived = false")
    boolean existsActiveBySuiteIdAndTitleIgnoreCase(@Param("projectId") Long projectId, @Param("suiteId") Long suiteId, @Param("title") String title);

    /**
     * Finds all non-archived test cases in a project, ordered by sort index and creation date.
     *
     * @param projectId the ID of the project
     * @return List of non-archived test cases sorted by sortIndex ASC, createdAt ASC
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId = :projectId AND tc.archived = false ORDER BY tc.sortIndex ASC, tc.createdAt ASC")
    List<TestCase> findAllActiveByProjectId(@Param("projectId") Long projectId);

    /**
     * Finds all non-archived test cases in a suite, ordered by sort index and creation date.
     *
     * @param projectId the ID of the project
     * @param suiteId the ID of the suite
     * @return List of non-archived test cases in the suite, sorted by sortIndex ASC, createdAt ASC
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId = :projectId AND tc.suiteId = :suiteId AND tc.archived = false ORDER BY tc.sortIndex ASC, tc.createdAt ASC")
    List<TestCase> findAllActiveBySuiteId(@Param("projectId") Long projectId, @Param("suiteId") Long suiteId);

    /**
     * Finds a non-archived test case by ID.
     *
     * @param id the test case ID
     * @return Optional containing the test case if found and not archived, empty otherwise
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.id = :id AND tc.archived = false")
    Optional<TestCase> findActiveById(@Param("id") Long id);

    /**
     * Finds all test cases in a project (including archived), ordered by sort index.
     *
     * @param projectId the ID of the project
     * @return List of all test cases sorted by sortIndex ASC
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId = :projectId ORDER BY tc.sortIndex ASC")
    List<TestCase> findAllByProjectIdOrderBySortIndex(@Param("projectId") Long projectId);

    /**
     * Finds all test cases in a project (including archived).
     *
     * @param projectId the ID of the project
     * @return List of all test cases
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId = :projectId")
    List<TestCase> findAllByProjectId(@Param("projectId") Long projectId);

    /**
     * Finds all non-archived test cases in a project.
     *
     * @param projectId the ID of the project
     * @return List of non-archived test cases
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId = :projectId AND tc.archived = false")
    List<TestCase> findActiveByProjectId(@Param("projectId") Long projectId);

    /**
     * Finds all non-archived test cases for multiple projects (for dashboard PDF export).
     *
     * @param projectIds list of project IDs
     * @return List of non-archived test cases from all specified projects
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.projectId IN :projectIds AND tc.archived = false")
    List<TestCase> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    /**
     * Finds all non-archived test cases for multiple suites.
     *
     * @param suiteIds list of suite IDs
     * @return List of non-archived test cases from all specified suites
     */
    @Query("SELECT tc FROM TestCase tc WHERE tc.suiteId IN :suiteIds AND tc.archived = false")
    List<TestCase> findAllActiveBySuiteIdIn(@Param("suiteIds") List<Long> suiteIds);
}

