package com.test.system.dto.project.response;

import java.util.List;

public record BulkArchiveProjectsResponse(
        List<Long> archivedIds,
        List<Long> alreadyArchivedIds,
        List<Long> notFoundIds
) {}

