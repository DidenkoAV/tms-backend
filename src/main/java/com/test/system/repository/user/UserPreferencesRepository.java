package com.test.system.repository.user;

import com.test.system.model.user.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing user preferences.
 */
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    /**
     * Finds user preferences by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the preferences if found, empty otherwise
     */
    Optional<UserPreferences> findByUserId(Long userId);
}

