package com.test.system.dto.milestone;

import java.time.Instant;

public record CreateMilestoneRequest(Long projectId, String name, String description,
                                     Instant startDate, Instant dueDate) {}
