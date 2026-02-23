package com.test.system.repository.milestone;

import com.test.system.model.milestone.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    /**
     * Finds a non-archived milestone by ID.
     *
     * @param id the milestone ID
     * @return Optional containing the milestone if found and not archived, empty otherwise
     */
    Optional<Milestone> findByIdAndArchivedFalse(Long id);

    /**
     * Finds all non-archived milestones for a project, ordered by creation date descending.
     *
     * @param projectId the ID of the project
     * @return List of non-archived milestones sorted by creation date (newest first)
     */
    List<Milestone> findAllByProjectIdAndArchivedFalseOrderByCreatedAtDesc(Long projectId);

    /**
     * Finds all non-archived milestones for multiple projects (for dashboard PDF export).
     *
     * @param projectIds list of project IDs
     * @return List of non-archived milestones from all specified projects
     */
    List<Milestone> findByProjectIdIn(List<Long> projectIds);
}

