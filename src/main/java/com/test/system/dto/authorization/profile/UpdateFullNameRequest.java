package com.test.system.dto.authorization.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFullNameRequest(
        @NotBlank @Size(min = 1, max = 200) String fullName
) {}

