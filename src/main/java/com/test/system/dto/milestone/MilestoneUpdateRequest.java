package com.test.system.dto.milestone;

import java.time.Instant;

public record MilestoneUpdateRequest(String name, String description,
                                     Boolean closed, Instant startDate, Instant dueDate) {}
