package com.test.system.service.milestone;

import com.test.system.dto.milestone.CreateMilestoneRequest;
import com.test.system.dto.milestone.MilestoneResponse;
import com.test.system.dto.milestone.MilestoneUpdateRequest;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.milestone.Milestone;
import com.test.system.model.user.User;
import com.test.system.repository.milestone.MilestoneRepository;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.utils.SecurityUtils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneService {

    private static final String LOG_PREFIX = "[Milestone]";

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public MilestoneResponse createMilestone(CreateMilestoneRequest request) {
        log.info("{} createMilestone: projectId={}", LOG_PREFIX, request.projectId());

        validateProjectExists(request.projectId());
        User currentUser = getCurrentUser();

        Instant now = Instant.now();

        Milestone milestone = Milestone.builder()
                .projectId(request.projectId())
                .name(request.name().trim())
                .description(StringUtils.emptyToNull(request.description()))
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .closed(false)
                .archived(false)
                .createdBy(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Milestone saved = milestoneRepository.save(milestone);
        log.info("{} created: milestoneId={}, projectId={}", LOG_PREFIX, saved.getId(), saved.getProjectId());

        return toResponse(saved, currentUser);
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> listMilestonesByProject(Long projectId) {
        log.info("{} listMilestonesByProject: projectId={}", LOG_PREFIX, projectId);

        validateProjectExists(projectId);

        List<Milestone> milestones = milestoneRepository
                .findAllByProjectIdAndArchivedFalseOrderByCreatedAtDesc(projectId);

        // Load all authors in one query to avoid N+1 problem
        List<Long> authorIds = milestones.stream()
                .map(Milestone::getCreatedBy)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, User> authors = UserUtils.loadUsersByIds(userRepository, authorIds);

        List<MilestoneResponse> result = milestones.stream()
                .map(m -> toResponse(m, authors.get(m.getCreatedBy())))
                .toList();

        log.info("{} listMilestonesByProject: projectId={}, count={}", LOG_PREFIX, projectId, result.size());

        return result;
    }

    @Transactional(readOnly = true)
    public MilestoneResponse getMilestone(Long id) {
        log.info("{} getMilestone: milestoneId={}", LOG_PREFIX, id);

        Milestone milestone = findActiveMilestone(id);
        User author = loadAuthor(milestone.getCreatedBy());

        return toResponse(milestone, author);
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long id, MilestoneUpdateRequest request) {
        log.info("{} updateMilestone: milestoneId={}", LOG_PREFIX, id);

        Milestone milestone = findActiveMilestone(id);
        applyUpdates(milestone, request);
        milestone.setUpdatedAt(Instant.now());

        User author = loadAuthor(milestone.getCreatedBy());
        return toResponse(milestone, author);
    }

    @Transactional
    public void archiveMilestone(Long id) {
        log.info("{} archiveMilestone: milestoneId={}", LOG_PREFIX, id);

        Milestone milestone = findActiveMilestone(id);

        Instant now = Instant.now();
        milestone.setArchived(true);
        milestone.setArchivedAt(now);
        milestone.setUpdatedAt(now);

        log.info("{} archived: milestoneId={}", LOG_PREFIX, id);
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    /**
     * Gets the current authenticated user.
     */
    private User getCurrentUser() {
        String email = SecurityUtils.currentEmail()
                .orElseThrow(() -> new IllegalStateException("Authenticated user required"));

        return UserUtils.findUserByEmail(userRepository, email);
    }

    /**
     * Validates that project exists and is active.
     */
    private void validateProjectExists(Long projectId) {
        projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    /**
     * Finds active (non-archived) milestone by ID.
     */
    private Milestone findActiveMilestone(Long id) {
        return milestoneRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
    }

    /**
     * Loads author by ID, returns null if not found.
     */
    private User loadAuthor(Long authorId) {
        if (authorId == null) {
            return null;
        }
        return userRepository.findById(authorId).orElse(null);
    }

    /**
     * Applies updates from request to milestone entity.
     * Only updates non-null fields (PATCH semantics).
     */
    private void applyUpdates(Milestone milestone, MilestoneUpdateRequest request) {
        if (request.name() != null && !request.name().isBlank()) {
            milestone.setName(request.name().trim());
        }
        if (request.description() != null) {
            milestone.setDescription(StringUtils.emptyToNull(request.description()));
        }
        if (request.closed() != null) {
            milestone.setClosed(request.closed());
        }
        if (request.startDate() != null) {
            milestone.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            milestone.setDueDate(request.dueDate());
        }
    }

    /**
     * Converts Milestone entity to response DTO.
     * Author can be null if not loaded or not found.
     */
    private MilestoneResponse toResponse(Milestone m, User author) {
        String authorName = (author == null)
                ? null
                : StringUtils.safeName(author.getFullName(), author.getEmail());

        String authorEmail = (author == null) ? null : author.getEmail();

        return new MilestoneResponse(
                m.getId(),
                m.getProjectId(),
                m.getName(),
                m.getDescription(),
                m.isClosed(),
                m.isArchived(),
                m.getStartDate(),
                m.getDueDate(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                m.getCreatedBy(),
                authorName,
                authorEmail
        );
    }
}
