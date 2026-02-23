package com.test.system.dto.group.invitation;

import jakarta.validation.constraints.NotBlank;

public record InviteMemberRequest(@NotBlank String email) {}

