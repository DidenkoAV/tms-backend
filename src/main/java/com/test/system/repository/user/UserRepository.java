package com.test.system.repository.user;

import com.test.system.model.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for managing User entities.
 * Provides methods for user lookup by email and ID, with optional eager loading of related entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by email address.
     *
     * @param email the email address to search for
     * @return Optional containing the user if found, empty otherwise
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * Checks if a user with the given email exists.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * Finds a user by email with all related entities eagerly loaded.
     * Loads: roles, memberships, and memberships.group.
     *
     * @param email the email address to search for
     * @return Optional containing the user with all relations loaded if found, empty otherwise
     */
    @EntityGraph(attributePaths = {"roles", "memberships", "memberships.group"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findWithAllByEmail(@Param("email") String email);

    /**
     * Finds a user by ID with all related entities eagerly loaded.
     * Loads: roles, memberships, and memberships.group.
     *
     * @param id the user ID to search for
     * @return Optional containing the user with all relations loaded if found, empty otherwise
     */
    @EntityGraph(attributePaths = {"roles", "memberships", "memberships.group"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findWithAllById(@Param("id") Long id);
}

