package com.test.system.repository.run;

import com.test.system.model.run.RunCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing test run cases (the many-to-many relationship between runs and test cases).
 * Each RunCase represents a test case included in a specific test run.
 */
public interface TestRunCaseRepository extends JpaRepository<RunCase, Long> {

    interface MilestoneStatusAggregate {
        Long getMilestoneId();
        Long getStatusId();
        Long getTotal();
    }

    interface RunStatusAggregate {
        Long getRunId();
        Long getStatusId();
        Long getTotal();
    }

    /**
     * Checks if a specific test case is included in a run.
     *
     * @param runId the run ID
     * @param caseId the test case ID
     * @return true if the case is in the run, false otherwise
     */
    boolean existsByRunIdAndCaseId(Long runId, Long caseId);

    /**
     * Finds a run case by run ID and test case ID.
     *
     * @param runId the run ID
     * @param caseId the test case ID
     * @return Optional containing the run case if found, empty otherwise
     */
    Optional<RunCase> findByRunIdAndCaseId(Long runId, Long caseId);

    /**
     * Finds all active (non-archived) test cases in a run.
     * Excludes archived test cases even if they were added to the run before archiving.
     *
     * @param runId the run ID
     * @return List of run cases with non-archived test cases
     */
    @Query("""
           SELECT rc
           FROM RunCase rc
           JOIN TestCase tc ON tc.id = rc.caseId
           WHERE rc.runId = :runId
             AND (tc.archived = false OR tc.archived IS NULL)
           """)
    List<RunCase> findActiveCasesByRunId(@Param("runId") Long runId);

    /**
     * Finds all run cases for a specific run (including archived test cases).
     *
     * @param runId the run ID
     * @return List of all run cases in the run
     */
    List<RunCase> findAllByRunId(Long runId);

    /**
     * Removes a specific test case from a run.
     *
     * @param runId the run ID
     * @param caseId the test case ID
     * @return number of run cases deleted (0 or 1)
     */
    int deleteByRunIdAndCaseId(Long runId, Long caseId);

    /**
     * Removes multiple test cases from a run in a single operation.
     *
     * @param runId the run ID
     * @param caseIds collection of test case IDs to remove
     * @return number of run cases deleted
     */
    int deleteByRunIdAndCaseIdIn(Long runId, Collection<Long> caseIds);

    /**
     * Deletes all run cases associated with a specific test case.
     * Used when a test case is permanently deleted.
     *
     * @param caseId the test case ID
     * @return number of run cases deleted
     */
    int deleteByCaseId(Long caseId);

    /**
     * Deletes all run cases associated with multiple test cases.
     * Used for bulk test case deletion.
     *
     * @param caseIds collection of test case IDs
     * @return number of run cases deleted
     */
    int deleteByCaseIdIn(Collection<Long> caseIds);

    /**
     * Counts run cases grouped by milestone and status for all active milestones/runs in a project.
     *
     * @param projectId the project ID
     * @return aggregated rows: milestoneId + statusId + total
     */
    @Query("""
           SELECT m.id AS milestoneId,
                  rc.currentStatusId AS statusId,
                  COUNT(rc.id) AS total
           FROM Milestone m
           JOIN m.runs r
           JOIN RunCase rc ON rc.runId = r.id
           WHERE m.projectId = :projectId
             AND m.archived = false
             AND r.archived = false
             AND rc.currentStatusId IS NOT NULL
           GROUP BY m.id, rc.currentStatusId
           """)
    List<MilestoneStatusAggregate> countMilestoneStatusCountsByProjectId(@Param("projectId") Long projectId);

    /**
     * Counts run cases grouped by run and status for all active runs in a project.
     *
     * @param projectId the project ID
     * @return aggregated rows: runId + statusId + total
     */
    @Query("""
           SELECT r.id AS runId,
                  rc.currentStatusId AS statusId,
                  COUNT(rc.id) AS total
           FROM Run r
           JOIN RunCase rc ON rc.runId = r.id
           JOIN TestCase tc ON tc.id = rc.caseId
           WHERE r.projectId = :projectId
             AND r.archived = false
             AND rc.currentStatusId IS NOT NULL
             AND (tc.archived = false OR tc.archived IS NULL)
           GROUP BY r.id, rc.currentStatusId
           """)
    List<RunStatusAggregate> countRunStatusCountsByProjectId(@Param("projectId") Long projectId);
}
