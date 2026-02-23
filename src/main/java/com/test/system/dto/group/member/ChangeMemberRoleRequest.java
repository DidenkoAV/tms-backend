package com.test.system.dto.group.member;

import jakarta.validation.constraints.NotBlank;

public record ChangeMemberRoleRequest(@NotBlank String role) {}

