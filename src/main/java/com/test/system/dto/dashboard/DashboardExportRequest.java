package com.test.system.dto.dashboard;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DashboardExportRequest(
    @NotNull(message = "Project IDs are required")
    List<Long> projectIds
) {
}

