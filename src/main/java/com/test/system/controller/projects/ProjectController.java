package com.test.system.controller.projects;

import com.test.system.dto.project.request.BulkArchiveProjectsRequest;
import com.test.system.dto.project.request.CreateProjectRequest;
import com.test.system.dto.project.request.UpdateProjectRequest;
import com.test.system.dto.project.response.BulkArchiveProjectsResponse;
import com.test.system.dto.project.response.ProjectResponse;
import com.test.system.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Project Controller", description = "Manage projects within a group")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/groups/{groupId}/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "Create project", description = "Create a new project in the group.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@PathVariable Long groupId,
                                  @Valid @RequestBody CreateProjectRequest request,
                                  @AuthenticationPrincipal UserDetails principal) {
        return projectService.createProject(groupId, principal.getUsername(), request);
    }

    @Operation(summary = "List projects", description = "List all active (non-archived) projects in the group.")
    @GetMapping
    public List<ProjectResponse> list(@PathVariable Long groupId,
                                      @AuthenticationPrincipal UserDetails principal) {
        return projectService.listActiveProjectsByGroup(groupId, principal.getUsername());
    }

    @Operation(summary = "Get project", description = "Get details for a single project.")
    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable Long groupId,
                               @PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal) {
        return projectService.getProject(groupId, id, principal.getUsername());
    }

    @Operation(summary = "Update project", description = "Update project name/description.")
    @PatchMapping("/{id}")
    public ProjectResponse update(@PathVariable Long groupId,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateProjectRequest request,
                                  @AuthenticationPrincipal UserDetails principal) {
        return projectService.updateProject(groupId, id, principal.getUsername(), request);
    }

    @Operation(summary = "Archive project", description = "Archive project in the group (MAINTAINER+).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long groupId,
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails principal) {
        projectService.archiveProject(groupId, id, principal.getUsername());
    }

    @Operation(summary = "Bulk archive projects", description = "Archive multiple projects by IDs (MAINTAINER+).")
    @PostMapping("/bulk-archive")
    public BulkArchiveProjectsResponse bulkArchive(@PathVariable Long groupId,
                                                  @Valid @RequestBody BulkArchiveProjectsRequest request,
                                                  @AuthenticationPrincipal UserDetails principal) {
        return projectService.archiveProjectsBulk(groupId, request.ids(), principal.getUsername());
    }
}
