package com.test.system.repository.suite;

import com.test.system.model.suite.Suite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TestSuiteRepository extends JpaRepository<Suite, Long> {

    /**
     * Checks if a non-archived suite with the given name exists in the project (case-insensitive).
     *
     * @param projectId the ID of the project
     * @param name the suite name to check
     * @return true if a non-archived suite with this name exists in the project, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Suite s " +
           "WHERE s.projectId = :projectId AND LOWER(s.name) = LOWER(:name) AND s.archived = false")
    boolean existsActiveByProjectIdAndNameIgnoreCase(@Param("projectId") Long projectId, @Param("name") String name);

    /**
     * Finds all non-archived suites for a project, ordered by creation date descending.
     *
     * @param projectId the ID of the project
     * @return List of non-archived suites sorted by creation date (newest first)
     */
    @Query("SELECT s FROM Suite s WHERE s.projectId = :projectId AND s.archived = false ORDER BY s.createdAt DESC")
    List<Suite> findAllActiveByProjectId(@Param("projectId") Long projectId);

    /**
     * Finds a non-archived suite by ID.
     *
     * @param id the suite ID
     * @return Optional containing the suite if found and not archived, empty otherwise
     */
    @Query("SELECT s FROM Suite s WHERE s.id = :id AND s.archived = false")
    Optional<Suite> findActiveById(@Param("id") Long id);

    /**
     * Finds all non-archived suites for multiple projects (for dashboard PDF export).
     *
     * @param projectIds list of project IDs
     * @return List of non-archived suites from all specified projects
     */
    @Query("SELECT s FROM Suite s WHERE s.projectId IN :projectIds AND s.archived = false")
    List<Suite> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}

