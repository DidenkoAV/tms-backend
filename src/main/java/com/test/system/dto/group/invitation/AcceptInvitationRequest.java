package com.test.system.dto.group.invitation;

import jakarta.validation.constraints.NotBlank;

public record AcceptInvitationRequest(@NotBlank String token) {}

