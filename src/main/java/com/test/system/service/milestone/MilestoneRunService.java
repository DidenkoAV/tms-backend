package com.test.system.service.milestone;

import com.test.system.dto.milestone.AddRunsToMilestoneRequest;
import com.test.system.dto.milestone.MilestoneStatusCountResponse;
import com.test.system.dto.run.response.RunResponse;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.milestone.Milestone;
import com.test.system.model.run.Run;
import com.test.system.model.user.User;
import com.test.system.repository.milestone.MilestoneRepository;
import com.test.system.repository.run.TestRunCaseRepository;
import com.test.system.repository.run.TestRunRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.utils.StringUtils;
import com.test.system.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneRunService {

    private static final String LOG_PREFIX = "[MilestoneRun]";

    private final MilestoneRepository milestoneRepository;
    private final TestRunRepository runRepository;
    private final TestRunCaseRepository runCaseRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RunResponse> listRunsByMilestone(Long milestoneId) {
        log.info("{} listRunsByMilestone: milestoneId={}", LOG_PREFIX, milestoneId);

        Milestone milestone = findActiveMilestone(milestoneId);

        List<Run> runs = runRepository
                .findAllActiveByMilestoneId(milestone.getId());

        // Load all authors in one query to avoid N+1 problem
        Map<Long, User> authors = loadAuthorsForRuns(runs);

        List<RunResponse> result = runs.stream()
                .map(r -> toResponse(r, authors.get(r.getCreatedBy())))
                .toList();

        log.info("{} listRunsByMilestone: milestoneId={}, count={}", LOG_PREFIX, milestoneId, result.size());

        return result;
    }

    @Transactional
    public List<RunResponse> addRunsToMilestone(Long milestoneId, AddRunsToMilestoneRequest request) {
        log.info("{} addRunsToMilestone: milestoneId={}, runIds={}", LOG_PREFIX, milestoneId, request.runIds());

        validateRunIdsNotEmpty(request.runIds());

        Milestone milestone = findActiveMilestone(milestoneId);
        List<Run> runs = findActiveRunsByIds(request.runIds());

        validateRunsNotEmpty(runs);
        validateRunsProjectMatch(runs, milestone.getProjectId());

        linkRunsToMilestone(milestone, runs);

        milestoneRepository.save(milestone);

        log.info("{} addRunsToMilestone: milestoneId={}, added={}", LOG_PREFIX, milestoneId, runs.size());

        return listRunsByMilestone(milestoneId);
    }

    @Transactional
    public void removeRunFromMilestone(Long milestoneId, Long runId) {
        log.info("{} removeRunFromMilestone: milestoneId={}, runId={}", LOG_PREFIX, milestoneId, runId);

        Milestone milestone = findActiveMilestone(milestoneId);
        Run run = findActiveRun(runId);

        unlinkRunFromMilestone(milestone, run);

        milestoneRepository.save(milestone);

        log.info("{} removeRunFromMilestone: milestoneId={}, runId={}, removed", LOG_PREFIX, milestoneId, runId);
    }

    @Transactional(readOnly = true)
    public List<MilestoneStatusCountResponse> listMilestoneStatusCountsByProject(Long projectId) {
        log.info("{} listMilestoneStatusCountsByProject: projectId={}", LOG_PREFIX, projectId);

        List<MilestoneStatusCountResponse> result = runCaseRepository
                .countMilestoneStatusCountsByProjectId(projectId)
                .stream()
                .map(row -> new MilestoneStatusCountResponse(
                        row.getMilestoneId(),
                        row.getStatusId(),
                        row.getTotal()
                ))
                .toList();

        log.info("{} listMilestoneStatusCountsByProject: projectId={}, rows={}", LOG_PREFIX, projectId, result.size());

        return result;
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Finds active (non-archived) milestone by ID.
     */
    private Milestone findActiveMilestone(Long id) {
        return milestoneRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
    }

    /**
     * Finds active (non-archived) run by ID.
     */
    private Run findActiveRun(Long id) {
        return runRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Run not found"));
    }

    /**
     * Finds active runs by list of IDs.
     */
    private List<Run> findActiveRunsByIds(List<Long> runIds) {
        return runRepository.findAllActiveByIdIn(runIds);
    }

    /**
     * Validates that runIds list is not null or empty.
     */
    private void validateRunIdsNotEmpty(List<Long> runIds) {
        if (runIds == null || runIds.isEmpty()) {
            throw new IllegalArgumentException("runIds must not be empty");
        }
    }

    /**
     * Validates that runs list is not empty.
     */
    private void validateRunsNotEmpty(List<Run> runs) {
        if (runs.isEmpty()) {
            throw new IllegalArgumentException("No valid runIds provided");
        }
    }

    /**
     * Validates that all runs belong to the same project as milestone.
     */
    private void validateRunsProjectMatch(List<Run> runs, Long milestoneProjectId) {
        for (Run run : runs) {
            if (!Objects.equals(run.getProjectId(), milestoneProjectId)) {
                throw new IllegalArgumentException(
                        String.format("Run %d project must match milestone project %d",
                                run.getId(), milestoneProjectId)
                );
            }
        }
    }

    /**
     * Links runs to milestone (bidirectional relationship).
     * Updates run.updatedAt timestamp.
     */
    private void linkRunsToMilestone(Milestone milestone, List<Run> runs) {
        Instant now = Instant.now();

        for (Run run : runs) {
            // Add to milestone's runs collection
            if (!milestone.getRuns().contains(run)) {
                milestone.getRuns().add(run);
            }

            // Add to run's milestones collection (bidirectional)
            if (!run.getMilestones().contains(milestone)) {
                run.getMilestones().add(milestone);
            }

            run.setUpdatedAt(now);
        }
    }

    /**
     * Unlinks run from milestone (bidirectional relationship).
     * Updates run.updatedAt timestamp if unlinked.
     */
    private void unlinkRunFromMilestone(Milestone milestone, Run run) {
        if (milestone.getRuns().remove(run)) {
            run.getMilestones().remove(milestone);
            run.setUpdatedAt(Instant.now());
        }
    }

    /**
     * Loads authors for list of runs in one query to avoid N+1 problem.
     */
    private Map<Long, User> loadAuthorsForRuns(List<Run> runs) {
        List<Long> authorIds = runs.stream()
                .map(Run::getCreatedBy)
                .filter(Objects::nonNull)
                .toList();

        return UserUtils.loadUsersByIds(userRepository, authorIds);
    }

    /**
     * Converts Run entity to response DTO.
     * Author can be null if not loaded or not found.
     */
    private RunResponse toResponse(Run r, User author) {
        String authorName = (author == null)
                ? null
                : StringUtils.safeName(author.getFullName(), author.getEmail());

        String authorEmail = (author == null) ? null : author.getEmail();

        return new RunResponse(
                r.getId(),
                r.getProjectId(),
                r.getName(),
                r.getDescription(),
                r.isClosed(),
                r.isArchived(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCreatedBy(),
                authorName,
                authorEmail
        );
    }
}
