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
     * Checks if a non-archived suite with the given name exists under the same parent (case-insensitive).
     *
     * @param projectId the ID of the project
     * @param parentId the parent suite ID (null for root level)
     * @param name the suite name to check
     * @return true if a non-archived suite with this name exists under the same parent, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Suite s " +
           "WHERE s.projectId = :projectId " +
           "AND (:parentId IS NULL AND s.parentId IS NULL OR s.parentId = :parentId) " +
           "AND LOWER(s.name) = LOWER(:name) AND s.archived = false")
    boolean existsActiveByProjectIdAndParentIdAndNameIgnoreCase(
            @Param("projectId") Long projectId,
            @Param("parentId") Long parentId,
            @Param("name") String name
    );

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

    /**
     * Finds all non-archived child suites of a parent suite.
     *
     * @param parentId the parent suite ID
     * @return List of non-archived child suites
     */
    @Query("SELECT s FROM Suite s WHERE s.parentId = :parentId AND s.archived = false ORDER BY s.createdAt DESC")
    List<Suite> findAllActiveByParentId(@Param("parentId") Long parentId);

    /**
     * Finds all non-archived suites by IDs.
     *
     * @param ids list of suite IDs
     * @return List of non-archived suites
     */
    @Query("SELECT s FROM Suite s WHERE s.id IN :ids AND s.archived = false")
    List<Suite> findAllActiveByIdIn(@Param("ids") List<Long> ids);

    /**
     * Finds all non-archived child suites recursively for given parent IDs.
     *
     * @param parentIds list of parent suite IDs
     * @return List of all child suites
     */
    @Query("SELECT s FROM Suite s WHERE s.parentId IN :parentIds AND s.archived = false")
    List<Suite> findAllActiveByParentIdIn(@Param("parentIds") List<Long> parentIds);
}

