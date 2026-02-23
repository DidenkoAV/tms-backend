package com.test.system.repository.run;

import com.test.system.model.run.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Repository for managing test execution results.
 * Each result represents a single test execution attempt with status, comment, and elapsed time.
 */
public interface TestResultRepository extends JpaRepository<Result, Long> {

    /**
     * Finds all results for a specific run case, ordered by creation time (oldest first).
     * This shows the execution history for a test case within a run.
     *
     * @param runCaseId the ID of the run case
     * @return List of results sorted by creation time (oldest first)
     */
    List<Result> findAllByRunCaseIdOrderByCreatedAtAsc(Long runCaseId);

    /**
     * Deletes multiple results by their IDs in a single batch operation.
     * More efficient than deleting one by one.
     *
     * @param ids collection of result IDs to delete
     * @return number of results deleted
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Result r WHERE r.id IN :ids")
    int deleteBulk(@Param("ids") Collection<Long> ids);
}

