package com.test.system.repository.project;

import com.test.system.model.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Checks if a project with the given code exists in the group (case-insensitive).
     * Includes both archived and non-archived projects.
     *
     * @param groupId the ID of the group
     * @param code the project code to check
     * @return true if a project with this code exists in the group, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p " +
           "WHERE p.group.id = :groupId AND LOWER(p.code) = LOWER(:code)")
    boolean existsByGroupIdAndCodeIgnoreCase(@Param("groupId") Long groupId, @Param("code") String code);

    /**
     * Checks if a non-archived project with the given name exists in the group (case-insensitive).
     *
     * @param groupId the ID of the group
     * @param name the project name to check
     * @return true if a non-archived project with this name exists in the group, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p " +
           "WHERE p.group.id = :groupId AND LOWER(p.name) = LOWER(:name) AND p.archived = false")
    boolean existsActiveByGroupIdAndNameIgnoreCase(@Param("groupId") Long groupId, @Param("name") String name);

    /**
     * Checks if a non-archived project with the given name exists in the group,
     * excluding a specific project ID (case-insensitive).
     * Useful for validating unique names during project updates.
     *
     * @param groupId the ID of the group
     * @param name the project name to check
     * @param excludeProjectId the project ID to exclude from the check
     * @return true if another non-archived project with this name exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p " +
           "WHERE p.group.id = :groupId AND LOWER(p.name) = LOWER(:name) " +
           "AND p.id <> :excludeProjectId AND p.archived = false")
    boolean existsActiveByGroupIdAndNameIgnoreCaseExcludingId(
            @Param("groupId") Long groupId,
            @Param("name") String name,
            @Param("excludeProjectId") Long excludeProjectId);

    /**
     * Finds a non-archived project by ID and group ID.
     *
     * @param projectId the project ID
     * @param groupId the ID of the group
     * @return Optional containing the project if found and not archived, empty otherwise
     */
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.group.id = :groupId AND p.archived = false")
    Optional<Project> findActiveByIdAndGroupId(@Param("projectId") Long projectId, @Param("groupId") Long groupId);

    /**
     * Finds all non-archived projects in a group, ordered by creation date descending.
     *
     * @param groupId the ID of the group
     * @return List of non-archived projects sorted by creation date (newest first)
     */
    @Query("SELECT p FROM Project p WHERE p.group.id = :groupId AND p.archived = false ORDER BY p.createdAt DESC")
    List<Project> findAllActiveByGroupId(@Param("groupId") Long groupId);

    /**
     * Finds all non-archived projects across multiple groups, ordered by creation date descending.
     *
     * @param groupIds collection of group IDs
     * @return List of non-archived projects from all specified groups, sorted by creation date (newest first)
     */
    @Query("SELECT p FROM Project p WHERE p.group.id IN :groupIds AND p.archived = false ORDER BY p.createdAt DESC")
    List<Project> findAllActiveByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    /**
     * Finds a non-archived project by ID.
     *
     * @param projectId the project ID
     * @return Optional containing the project if found and not archived, empty otherwise
     */
    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.archived = false")
    Optional<Project> findActiveById(@Param("projectId") Long projectId);
}

