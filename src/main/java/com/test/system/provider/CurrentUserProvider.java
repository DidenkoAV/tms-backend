package com.test.system.provider;

import com.test.system.model.user.User;
import com.test.system.repository.user.UserRepository;
import com.test.system.utils.SecurityUtils;
import com.test.system.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provider for getting the current authenticated user.
 * Guarantees that a valid user is returned or throws a clear exception.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;

    /**
     * Require and return the current authenticated user.
     * This method guarantees that a valid user is returned.
     *
     * @return the current authenticated user (never null)
     * @throws IllegalStateException if no user is authenticated or user not found
     */
    public User requireCurrentUser() {
        String email = SecurityUtils.currentEmail()
                .orElseThrow(() -> new IllegalStateException("Authentication required: no user is currently authenticated"));
        return UserUtils.findUserByEmail(userRepository, email);
    }
}

