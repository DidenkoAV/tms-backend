package com.test.system.repository.run;

import com.test.system.model.run.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing test runs.
 * A test run represents a test execution session containing multiple test cases.
 */
public interface TestRunRepository extends JpaRepository<Run, Long> {

    /**
     * Finds a non-archived test run by ID.
     *
     * @param id the run ID
     * @return Optional containing the run if found and not archived, empty otherwise
     */
    @Query("SELECT r FROM Run r WHERE r.id = :id AND r.archived = false")
    Optional<Run> findActiveById(@Param("id") Long id);

    /**
     * Finds all non-archived test runs for a project, ordered by creation date descending.
     *
     * @param projectId the project ID
     * @return List of non-archived runs sorted by creation date (newest first)
     */
    @Query("SELECT r FROM Run r WHERE r.projectId = :projectId AND r.archived = false ORDER BY r.createdAt DESC")
    List<Run> findAllActiveByProjectId(@Param("projectId") Long projectId);

    /**
     * Finds all non-archived test runs associated with a milestone, ordered by creation date descending.
     *
     * @param milestoneId the milestone ID
     * @return List of non-archived runs sorted by creation date (newest first)
     */
    @Query("SELECT r FROM Run r JOIN r.milestones m WHERE m.id = :milestoneId AND r.archived = false ORDER BY r.createdAt DESC")
    List<Run> findAllActiveByMilestoneId(@Param("milestoneId") Long milestoneId);

    /**
     * Finds all test runs by their IDs (including archived).
     *
     * @param ids collection of run IDs
     * @return List of runs
     */
    List<Run> findAllByIdIn(Collection<Long> ids);

    /**
     * Finds all non-archived test runs by their IDs.
     *
     * @param ids collection of run IDs
     * @return List of non-archived runs
     */
    @Query("SELECT r FROM Run r WHERE r.id IN :ids AND r.archived = false")
    List<Run> findAllActiveByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * Archives multiple test runs in a single batch operation.
     * Sets archived=true, archivedAt, and updatedAt for all specified runs.
     *
     * @param ids collection of run IDs to archive
     * @param now the current timestamp
     * @return number of runs archived
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Run r
              SET r.archived = true,
                  r.archivedAt = :now,
                  r.updatedAt = :now
            WHERE r.id IN :ids
              AND r.archived = false
           """)
    int archiveBulk(@Param("ids") Collection<Long> ids, @Param("now") Instant now);

    /**
     * Finds all non-archived test runs for multiple projects (for dashboard PDF export).
     *
     * @param projectIds list of project IDs
     * @return List of non-archived runs from all specified projects
     */
    @Query("SELECT r FROM Run r WHERE r.projectId IN :projectIds AND r.archived = false ORDER BY r.createdAt DESC")
    List<Run> findByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}

