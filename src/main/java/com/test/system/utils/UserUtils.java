package com.test.system.utils;

import com.test.system.exceptions.common.NotFoundException;
import com.test.system.model.user.User;
import com.test.system.repository.user.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for working with User entities.
 */
public final class UserUtils {

    private UserUtils() {
        // Utility class
    }

    /**
     * Finds user by email (case-insensitive).
     * Normalizes email by trimming and converting to lowercase.
     *
     * @param userRepository the user repository
     * @param email          the email to search for
     * @return User entity
     * @throws NotFoundException if user not found
     */
    public static User findUserByEmail(UserRepository userRepository, String email) {
        String normalized = normalizeEmail(email);
        return userRepository.findByEmail(normalized)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Normalizes email by trimming and converting to lowercase.
     *
     * @param email the email to normalize
     * @return normalized email
     */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Loads users by their IDs and returns them as a map.
     * Filters out null IDs before loading.
     *
     * @param userRepository repository to load users from
     * @param ids            collection of user IDs (can contain nulls)
     * @return map of userId -> User, empty map if no valid IDs
     */
    public static Map<Long, User> loadUsersByIds(UserRepository userRepository, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> validIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(validIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }
}

