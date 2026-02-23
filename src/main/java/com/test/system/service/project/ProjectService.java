package com.test.system.service.project;

import com.test.system.dto.project.request.CreateProjectRequest;
import com.test.system.dto.project.request.UpdateProjectRequest;
import com.test.system.dto.project.response.BulkArchiveProjectsResponse;
import com.test.system.dto.project.response.ProjectResponse;
import com.test.system.dto.project.response.ProjectSummaryResponse;
import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.exceptions.common.Forbidden;
import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.group.Group;
import com.test.system.model.group.GroupMembership;
import com.test.system.model.project.Project;
import com.test.system.model.user.User;
import com.test.system.repository.project.ProjectRepository;
import com.test.system.repository.user.UserRepository;
import com.test.system.repository.group.GroupMembershipRepository;
import com.test.system.repository.group.GroupRepository;
import com.test.system.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private static final String LOG_PREFIX = "[Project]";
    private static final int MIN_NAME_LEN = 3;

    private final ProjectRepository projectRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * List all active projects across all groups where user is an active member.
     */
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> listAllProjectsForUser(String requesterEmail) {
        log.info("{} listAllProjectsForUser: user={}", LOG_PREFIX, requesterEmail);

        User user = findUserByEmail(requesterEmail);
        List<Long> groupIds = findActiveGroupIdsForUser(user.getId());

        if (groupIds.isEmpty()) {
            log.info("{} listAllProjectsForUser: user={} has no active groups", LOG_PREFIX, requesterEmail);
            return List.of();
        }

        List<ProjectSummaryResponse> result = projectRepository
                .findAllActiveByGroupIds(groupIds)
                .stream()
                .filter(p -> p.getGroup() != null)
                .map(this::toProjectSummary)
                .toList();

        log.info("{} listAllProjectsForUser: user={}, groups={}, count={}",
                LOG_PREFIX, requesterEmail, groupIds.size(), result.size());

        return result;
    }

    /**
     * Create a new project in a group (MAINTAINER+).
     */
    @Transactional
    public ProjectResponse createProject(Long groupId, String requesterEmail, CreateProjectRequest request) {
        log.info("{} createProject: groupId={}, user={}, name={}, code={}",
                LOG_PREFIX, groupId, requesterEmail, request.name(), request.code());

        User user = findUserByEmail(requesterEmail);
        Group group = findGroup(groupId);

        validateUserHasRole(groupId, user.getId(), GroupRole.MAINTAINER);

        String name = normalizeString(request.name());
        String code = normalizeString(request.code());
        String description = request.description(); // nullable is allowed

        validateProjectNameUnique(groupId, name);
        validateProjectCodeUnique(groupId, code);

        Project project = Project.builder()
                .group(group)
                .name(name)
                .code(code)
                .description(description)
                .build();

        Project saved = projectRepository.save(project);

        log.info("{} createProject: success groupId={}, projectId={}", LOG_PREFIX, groupId, saved.getId());
        return toProjectResponse(saved);
    }

    /**
     * Update project name/description (MAINTAINER+).
     */
    @Transactional
    public ProjectResponse updateProject(Long groupId,
                                         Long projectId,
                                         String requesterEmail,
                                         UpdateProjectRequest request) {

        log.info("{} updateProject: groupId={}, projectId={}, user={}",
                LOG_PREFIX, groupId, projectId, requesterEmail);

        User user = findUserByEmail(requesterEmail);
        validateUserHasRole(groupId, user.getId(), GroupRole.MAINTAINER);

        Project project = findActiveProject(projectId, groupId);

        applyProjectUpdates(project, request, groupId);
        project.setUpdatedAt(Instant.now());

        log.info("{} updateProject: success groupId={}, projectId={}", LOG_PREFIX, groupId, projectId);
        return toProjectResponse(project);
    }

    /**
     * List active (non-archived) projects for a group.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listActiveProjectsByGroup(Long groupId, String requesterEmail) {
        log.info("{} listActiveProjectsByGroup: groupId={}, user={}", LOG_PREFIX, groupId, requesterEmail);

        User user = findUserByEmail(requesterEmail);
        validateUserIsActiveMember(groupId, user.getId());

        List<ProjectResponse> result = projectRepository
                .findAllActiveByGroupId(groupId)
                .stream()
                .map(ProjectService::toProjectResponse)
                .toList();

        log.info("{} listActiveProjectsByGroup: groupId={}, count={}", LOG_PREFIX, groupId, result.size());
        return result;
    }

    /**
     * Get single project by id (active only).
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProject(Long groupId, Long projectId, String requesterEmail) {
        log.info("{} getProject: groupId={}, projectId={}, user={}",
                LOG_PREFIX, groupId, projectId, requesterEmail);

        User user = findUserByEmail(requesterEmail);
        validateUserIsActiveMember(groupId, user.getId());

        Project project = findActiveProject(projectId, groupId);

        log.info("{} getProject: success groupId={}, projectId={}", LOG_PREFIX, groupId, projectId);
        return toProjectResponse(project);
    }

    /**
     * Archive a single project (MAINTAINER+).
     */
    @Transactional
    public void archiveProject(Long groupId, Long projectId, String requesterEmail) {
        log.info("{} archiveProject: groupId={}, projectId={}, user={}",
                LOG_PREFIX, groupId, projectId, requesterEmail);

        User user = findUserByEmail(requesterEmail);
        validateUserHasRole(groupId, user.getId(), GroupRole.MAINTAINER);

        Project project = findActiveProject(projectId, groupId);

        markProjectAsArchived(project);

        log.info("{} archiveProject: success groupId={}, projectId={}", LOG_PREFIX, groupId, projectId);
    }

    /**
     * Archive multiple projects by IDs (MAINTAINER+).
     */
    @Transactional
    public BulkArchiveProjectsResponse archiveProjectsBulk(Long groupId,
                                                           List<Long> ids,
                                                           String requesterEmail) {
        log.info("{} archiveProjectsBulk: groupId={}, user={}, ids={}",
                LOG_PREFIX, groupId, requesterEmail, ids);

        validateProjectIdsNotEmpty(ids);

        User user = findUserByEmail(requesterEmail);
        validateUserHasRole(groupId, user.getId(), GroupRole.MAINTAINER);

        BulkArchiveProjectsResponse response = processBulkArchive(groupId, ids);

        log.info("{} archiveProjectsBulk: groupId={}, archived={}, alreadyArchived={}, notFound={}",
                LOG_PREFIX, groupId, response.archivedIds().size(),
                response.alreadyArchivedIds().size(), response.notFoundIds().size());

        return response;
    }

    // ========================================================================
    // Helper methods - Data loading
    // ========================================================================

    /**
     * Finds user by email (case-insensitive).
     */
    private User findUserByEmail(String email) {
        return UserUtils.findUserByEmail(userRepository, email);
    }

    /**
     * Finds group by ID.
     */
    private Group findGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));
    }

    /**
     * Finds active project by ID and group ID.
     */
    private Project findActiveProject(Long projectId, Long groupId) {
        return projectRepository
                .findActiveByIdAndGroupId(projectId, groupId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    /**
     * Finds all active group IDs where user has active membership.
     */
    private List<Long> findActiveGroupIdsForUser(Long userId) {
        return membershipRepository
                .findUserMembershipsByStatus(userId, MembershipStatus.ACTIVE)
                .stream()
                .map(m -> m.getGroup().getId())
                .distinct()
                .toList();
    }

    // ========================================================================
    // Helper methods - Authorization
    // ========================================================================

    /**
     * Validates that user is an active member of the group (any role).
     */
    private void validateUserIsActiveMember(Long groupId, Long userId) {
        findActiveMembership(groupId, userId);
    }

    /**
     * Validates that user has at least the specified role in the group.
     */
    private void validateUserHasRole(Long groupId, Long userId, GroupRole minRole) {
        GroupMembership membership = findActiveMembership(groupId, userId);
        if (getRoleRank(membership.getRole()) < getRoleRank(minRole)) {
            throw new Forbidden("Required role " + minRole + " or higher");
        }
    }

    /**
     * Finds active membership for user in group.
     */
    private GroupMembership findActiveMembership(Long groupId, Long userId) {
        GroupMembership membership = membershipRepository
                .findMembership(groupId, userId)
                .orElseThrow(() -> new Forbidden("User is not a member of this group"));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new Forbidden("Membership is not active");
        }
        return membership;
    }

    /**
     * Returns numeric rank for role comparison.
     */
    private static int getRoleRank(GroupRole role) {
        return switch (role) {
            case OWNER -> 3;
            case MAINTAINER -> 2;
            case MEMBER -> 1;
        };
    }

    // ========================================================================
    // Helper methods - Validation
    // ========================================================================

    /**
     * Validates that project IDs list is not empty.
     */
    private void validateProjectIdsNotEmpty(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }
    }

    /**
     * Validates that project name is unique in the group.
     */
    private void validateProjectNameUnique(Long groupId, String name) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        if (name.length() < MIN_NAME_LEN) {
            throw new IllegalArgumentException(
                    "Project name must be at least " + MIN_NAME_LEN + " characters"
            );
        }
        if (projectRepository.existsActiveByGroupIdAndNameIgnoreCase(groupId, name)) {
            throw new IllegalArgumentException("Project name already exists in this group");
        }
    }

    /**
     * Validates that project code is unique in the group.
     */
    private void validateProjectCodeUnique(Long groupId, String code) {
        if (code.isBlank()) {
            throw new IllegalArgumentException("Project code is required");
        }
        if (projectRepository.existsByGroupIdAndCodeIgnoreCase(groupId, code)) {
            throw new IllegalArgumentException("Project code already exists in this group");
        }
    }

    /**
     * Validates that updated project name is unique (excluding current project).
     */
    private void validateUpdatedNameUnique(Project project, String newName, Long groupId) {
        if (newName.length() < MIN_NAME_LEN) {
            throw new IllegalArgumentException(
                    "Project name must be at least " + MIN_NAME_LEN + " characters"
            );
        }

        boolean nameChanged = !newName.equalsIgnoreCase(project.getName());
        if (nameChanged &&
                projectRepository.existsActiveByGroupIdAndNameIgnoreCaseExcludingId(
                        groupId, newName, project.getId())) {
            throw new IllegalArgumentException("Project name already exists in this group");
        }
    }

    // ========================================================================
    // Helper methods - Business logic
    // ========================================================================

    /**
     * Applies updates from request to project entity.
     * Only updates non-null fields (PATCH semantics).
     */
    private void applyProjectUpdates(Project project, UpdateProjectRequest request, Long groupId) {
        // Update name if present
        if (request.name() != null) {
            String newName = normalizeString(request.name());
            if (!newName.isBlank()) {
                validateUpdatedNameUnique(project, newName, groupId);
                project.setName(newName);
            }
        }

        // Update description if present
        if (request.description() != null) {
            project.setDescription(request.description());
        }
    }

    /**
     * Marks project as archived with timestamp.
     */
    private void markProjectAsArchived(Project project) {
        Instant now = Instant.now();
        project.setArchived(true);
        project.setArchivedAt(now);
        project.setUpdatedAt(now);
    }

    /**
     * Processes bulk archive operation.
     * Returns response with archived, already archived, and not found IDs.
     */
    private BulkArchiveProjectsResponse processBulkArchive(Long groupId, List<Long> ids) {
        // Preserve order and remove duplicates
        Set<Long> requestedIds = new LinkedHashSet<>(ids);

        List<Project> found = projectRepository.findAllById(requestedIds);

        Set<Long> foundIds = new HashSet<>();
        List<Long> archivedIds = new ArrayList<>();
        List<Long> alreadyArchivedIds = new ArrayList<>();

        Instant now = Instant.now();

        for (Project project : found) {
            // Skip projects from other groups
            if (project.getGroup() == null || !groupId.equals(project.getGroup().getId())) {
                continue;
            }

            Long projectId = project.getId();
            foundIds.add(projectId);

            if (!project.isArchived()) {
                project.setArchived(true);
                project.setArchivedAt(now);
                project.setUpdatedAt(now);
                archivedIds.add(projectId);
            } else {
                alreadyArchivedIds.add(projectId);
            }
        }

        List<Long> notFoundIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        return new BulkArchiveProjectsResponse(
                archivedIds,
                alreadyArchivedIds,
                notFoundIds
        );
    }

    // ========================================================================
    // Helper methods - Mapping
    // ========================================================================

    /**
     * Normalizes string by trimming whitespace.
     */
    private static String normalizeString(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Converts Project entity to ProjectResponse DTO.
     */
    private static ProjectResponse toProjectResponse(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getCode(),
                p.getDescription(),
                p.isArchived()
        );
    }

    /**
     * Converts Project entity to ProjectSummaryResponse DTO.
     */
    private ProjectSummaryResponse toProjectSummary(Project p) {
        return new ProjectSummaryResponse(
                p.getId(),
                p.getName(),
                p.getCode(),
                p.getGroup().getId(),
                p.getGroup().getName()
        );
    }
}
