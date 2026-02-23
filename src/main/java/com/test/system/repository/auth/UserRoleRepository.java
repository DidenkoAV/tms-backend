package com.test.system.repository.auth;

import com.test.system.enums.auth.RoleName;
import com.test.system.model.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for managing user roles (ROLE_ADMIN, ROLE_USER).
 *
 * <p>Roles are pre-seeded in the database and rarely change.
 * This repository is primarily used during user registration and OAuth2 login
 * to assign the default ROLE_USER to new users.
 *
 * <p><b>Available roles:</b>
 * <ul>
 *   <li>{@link RoleName#ROLE_ADMIN} - Administrator with full system access</li>
 *   <li>{@link RoleName#ROLE_USER} - Regular user with standard permissions</li>
 * </ul>
 *
 * @see RoleName
 * @see com.test.system.model.user.User
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    /**
     * Find a role by its name.
     *
     * <p>This method is used during user registration to assign the default ROLE_USER,
     * and during OAuth2 login to ensure new users get the correct role.
     *
     * <p><b>Usage examples:</b>
     * <pre>{@code
     * // During user registration
     * UserRole roleUser = roleRepository.findByName(RoleName.ROLE_USER)
     *     .orElseThrow(() -> new IllegalStateException("ROLE_USER missing"));
     * user.getRoles().add(roleUser);
     *
     * // During admin assignment
     * UserRole roleAdmin = roleRepository.findByName(RoleName.ROLE_ADMIN)
     *     .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing"));
     * }</pre>
     *
     * @param name the role name to search for (ROLE_ADMIN or ROLE_USER)
     * @return the role if found, empty otherwise
     * @throws IllegalStateException if the role is missing (should never happen in production)
     * @see RoleName
     */
    Optional<UserRole> findByName(RoleName name);
}

