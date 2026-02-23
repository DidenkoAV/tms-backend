package com.test.system.dto.authorization.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 200) String fullName,
        @Email String newEmail
) {}

