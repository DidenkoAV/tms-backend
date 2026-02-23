package com.test.system.controller.projects;

import com.test.system.dto.project.response.ProjectSummaryResponse;
import com.test.system.service.project.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Projects All Controller", description = "List projects across all groups")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/projects")
public class ProjectsAllController {

    private final ProjectService projectService;

    @Operation(
            summary = "List all projects across my groups",
            description = "Returns all non-archived projects from every group where the current user is an ACTIVE member. Sorted by newest first."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProjectSummaryResponse.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/all")
    public List<ProjectSummaryResponse> listAll(@AuthenticationPrincipal UserDetails principal) {
        return projectService.listAllProjectsForUser(principal.getUsername());
    }
}
