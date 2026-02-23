package com.test.system.controller.authorization;

import com.test.system.dto.authorization.common.StatusResponse;
import com.test.system.dto.authorization.email.ChangeEmailRequest;
import com.test.system.dto.authorization.password.ChangePasswordRequest;
import com.test.system.dto.authorization.profile.UserProfileResponse;
import com.test.system.dto.authorization.profile.UpdateFullNameRequest;
import com.test.system.dto.authorization.profile.UpdateProfileRequest;
import com.test.system.dto.authorization.profile.UpdateProfileResponse;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.service.authorization.user.UserPasswordService;
import com.test.system.service.authorization.user.UserProfileService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.test.system.utils.auth.AuthWebUtils.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Auth Profile Controller", description = "Manage user profile and password")
@SecurityRequirement(name = "bearerAuth")
public class AuthProfileController {

    private final UserProfileService profileService;
    private final UserPasswordService passwordService;

    @PutMapping("/profile")
    public ResponseEntity<UpdateProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest body,
            Authentication auth
    ) {
        String email = requireEmail(auth);
        return ResponseEntity.ok(profileService.updateProfileComplete(email, body.fullName(), body.newEmail()));
    }

    @PostMapping("/profile/full-name")
    public ResponseEntity<StatusResponse> updateFullNameOnly(
            @RequestBody UpdateFullNameRequest body,
            Authentication auth
    ) {
        String email = requireEmail(auth);
        profileService.updateFullName(email, body.fullName());
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @PostMapping("/profile/email/change")
    public ResponseEntity<StatusResponse> startEmailChange(
            @RequestBody ChangeEmailRequest body,
            Authentication auth
    ) {
        String email = requireEmail(auth);
        profileService.startEmailChange(email, body.newEmail());
        return ResponseEntity.ok(StatusResponse.sent());
    }

    @PostMapping("/email/confirm")
    public ResponseEntity<StatusResponse> confirmNewEmail(@RequestParam("token") String token) {
        profileService.confirmEmailChange(token);
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @PostMapping("/password/change")
    public ResponseEntity<StatusResponse> changePassword(
            @RequestBody ChangePasswordRequest body,
            Authentication auth
    ) {
        String email = requireEmail(auth);
        passwordService.changePassword(email, body.currentPassword(), body.newPassword());
        return ResponseEntity.ok(StatusResponse.ok());
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Authentication auth) {
        String email = requireEmail(auth);
        return ResponseEntity.ok(profileService.getProfile(email));
    }

    private String requireEmail(Authentication auth) {
        String email = currentEmail(auth);
        if (email == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return email;
    }
}
