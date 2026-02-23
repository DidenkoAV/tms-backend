package com.test.system.controller.authorization;

import com.test.system.dto.authorization.token.CreateTokenRequest;
import com.test.system.dto.authorization.token.ApiTokenResponse;
import com.test.system.exceptions.auth.UnauthorizedException;
import com.test.system.service.authorization.pat.PersonalAccessTokenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.test.system.utils.auth.AuthWebUtils.currentEmail;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Token Controller", description = "Manage Personal Access Tokens (PAT)")
@SecurityRequirement(name = "bearerAuth")
public class AuthTokenController {

    private final PersonalAccessTokenService tokenService;

    @PostMapping("/tokens")
    public ResponseEntity<ApiTokenResponse> createToken(
            @RequestBody CreateTokenRequest req,
            Authentication auth
    ) {
        String email = requireEmail(auth);
        return ResponseEntity.ok(tokenService.createToken(email, req.name(), req.scopes()));
    }

    @GetMapping("/tokens")
    public ResponseEntity<List<ApiTokenResponse>> listTokens(Authentication auth) {
        String email = requireEmail(auth);
        return ResponseEntity.ok(tokenService.listActiveTokens(email));
    }

    @DeleteMapping("/tokens/{id}")
    public ResponseEntity<Void> revokeToken(@PathVariable String id, Authentication auth) {
        String email = requireEmail(auth);
        tokenService.revokeToken(email, id);
        return ResponseEntity.noContent().build();
    }

    private String requireEmail(Authentication auth) {
        String email = currentEmail(auth);
        if (email == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return email;
    }
}
