package com.test.system.dto.group.management;

import jakarta.validation.constraints.NotBlank;

public record RenameGroupRequest(@NotBlank String name) {}

