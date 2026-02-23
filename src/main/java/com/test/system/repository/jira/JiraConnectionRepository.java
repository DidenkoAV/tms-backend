package com.test.system.repository.jira;

import com.test.system.model.group.Group;
import com.test.system.model.jira.JiraConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JiraConnectionRepository extends JpaRepository<JiraConnection, Long> {

    /**
     * Finds a Jira connection by group entity.
     *
     * @param group the group entity to search by
     * @return Optional containing the Jira connection if found, empty otherwise
     */
    Optional<JiraConnection> findByGroup(Group group);

    /**
     * Finds a Jira connection by group ID.
     *
     * @param groupId the ID of the group
     * @return Optional containing the Jira connection if found, empty otherwise
     */
    Optional<JiraConnection> findByGroupId(Long groupId);
}

