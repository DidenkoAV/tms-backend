package com.test.system.controller.suite;

import com.test.system.dto.suite.SuiteCreateRequest;
import com.test.system.dto.suite.SuiteResponse;
import com.test.system.dto.suite.SuiteUpdateRequest;
import com.test.system.service.suite.SuiteService;
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
@Tag(name = "Suite Controller", description = "Manage test suites")
@SecurityRequirement(name = "bearerAuth")
public class SuiteController {

    private final SuiteService suiteService;

    @Operation(summary = "Create suite", description = "Create a suite in a project.")
    @PostMapping("/api/projects/{projectId}/suites")
    @ResponseStatus(HttpStatus.CREATED)
    public SuiteResponse create(@PathVariable Long projectId,
                                @Valid @RequestBody SuiteCreateRequest body) {
        return suiteService.create(projectId, body);
    }

    @Operation(summary = "List suites", description = "List non-archived suites for a project.")
    @GetMapping("/api/projects/{projectId}/suites")
    public List<SuiteResponse> listByProject(@PathVariable Long projectId) {
        return suiteService.listByProject(projectId);
    }

    @Operation(summary = "Get suite", description = "Get suite by id.")
    @GetMapping("/api/suites/{id}")
    public SuiteResponse get(@PathVariable Long id) {
        return suiteService.get(id);
    }

    @Operation(summary = "Update suite", description = "Update suite name/description.")
    @PatchMapping("/api/suites/{id}")
    public SuiteResponse update(@PathVariable Long id,
                                @Valid @RequestBody SuiteUpdateRequest request) {
        return suiteService.update(id, request);
    }

    @Operation(summary = "Archive suite", description = "Archive suite (soft delete).")
    @DeleteMapping("/api/suites/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        suiteService.archive(id);
    }
}
