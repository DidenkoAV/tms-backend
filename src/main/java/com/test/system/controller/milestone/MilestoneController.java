package com.test.system.controller.milestone;

import com.test.system.dto.milestone.AddRunsToMilestoneRequest;
import com.test.system.dto.milestone.CreateMilestoneRequest;
import com.test.system.dto.milestone.MilestoneResponse;
import com.test.system.dto.milestone.MilestoneUpdateRequest;
import com.test.system.dto.run.response.RunResponse;
import com.test.system.service.milestone.MilestoneRunService;
import com.test.system.service.milestone.MilestoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Milestone Controller", description = "Manage project milestones and milestone runs")
@SecurityRequirement(name = "bearerAuth")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final MilestoneRunService milestoneRunService;

    @Operation(summary = "Create milestone")
    @PostMapping("/api/projects/{projectId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    public MilestoneResponse create(@PathVariable Long projectId,
                                          @Valid @RequestBody CreateMilestoneRequest body) {
        CreateMilestoneRequest req = new CreateMilestoneRequest(
                projectId,
                body.name(),
                body.description(),
                body.startDate(),
                body.dueDate()
        );
        return milestoneService.createMilestone(req);
    }

    @Operation(summary = "List milestones for project")
    @GetMapping("/api/projects/{projectId}/milestones")
    public List<MilestoneResponse> list(@PathVariable Long projectId) {
        return milestoneService.listMilestonesByProject(projectId);
    }

    @Operation(summary = "Get milestone")
    @GetMapping("/api/milestones/{id}")
    public MilestoneResponse get(@PathVariable Long id) {
        return milestoneService.getMilestone(id);
    }

    @Operation(summary = "Update milestone")
    @PatchMapping("/api/milestones/{id}")
    public MilestoneResponse update(@PathVariable Long id,
                                          @Valid @RequestBody MilestoneUpdateRequest request) {
        return milestoneService.updateMilestone(id, request);
    }

    @Operation(summary = "Archive milestone")
    @DeleteMapping("/api/milestones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        milestoneService.archiveMilestone(id);
    }

    @Operation(summary = "List runs in milestone")
    @GetMapping("/api/milestones/{milestoneId}/runs")
    public List<RunResponse> listRuns(@PathVariable Long milestoneId) {
        return milestoneRunService.listRunsByMilestone(milestoneId);
    }

    @Operation(summary = "Add runs to milestone")
    @PostMapping("/api/milestones/{milestoneId}/runs")
    public List<RunResponse> addRuns(@PathVariable Long milestoneId,
                                           @Valid @RequestBody AddRunsToMilestoneRequest request) {
        return milestoneRunService.addRunsToMilestone(milestoneId, request);
    }

    @Operation(summary = "Remove run from milestone")
    @DeleteMapping("/api/milestones/{milestoneId}/runs/{runId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRun(@PathVariable Long milestoneId,
                          @PathVariable Long runId) {
        milestoneRunService.removeRunFromMilestone(milestoneId, runId);
    }
}
