package com.test.system.service.jira;

import com.test.system.client.JiraHttpClient;
import com.test.system.component.jira.JiraIssueMapper;
import com.test.system.dto.jira.connection.JiraConnectionResponse;
import com.test.system.dto.jira.connection.SaveConnectionRequest;
import com.test.system.model.jira.JiraConnection;
import com.test.system.repository.group.GroupRepository;
import com.test.system.repository.jira.JiraConnectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Manages Jira connections for groups.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraConnectionService {

    private final JiraConnectionRepository connections;
    private final GroupRepository groups;
    private final JiraHttpClient httpClient;
    private final JiraIssueMapper mapper;

    /**
     * Gets Jira connection for a group.
     */
    public JiraConnectionResponse getJiraConnection(Long groupId) {
        log.info("[JiraConn] Get: groupId={}", groupId);
        var conn = getConnectionEntity(groupId);
        return mapper.toConnectionDto(conn);
    }

    /**
     * Gets Jira connection for a group, returning null when not configured.
     */
    public JiraConnectionResponse getJiraConnectionOrNull(Long groupId) {
        return findConnectionEntity(groupId)
                .map(mapper::toConnectionDto)
                .orElse(null);
    }

    /**
     * Gets Jira connection entity (for internal use).
     */
    JiraConnection getConnectionEntity(Long groupId) {
        return connections.findByGroupId(groupId)
                .orElseThrow(() -> new EntityNotFoundException("No Jira connection for group " + groupId));
    }

    /**
     * Finds Jira connection entity without throwing when absent.
     */
    Optional<JiraConnection> findConnectionEntity(Long groupId) {
        return connections.findByGroupId(groupId);
    }

    /**
     * Saves or updates Jira connection.
     */
    @Transactional
    public JiraConnectionResponse saveJiraConnection(Long groupId, SaveConnectionRequest req) {
        log.info("[JiraConn] Save: groupId={}, url={}", groupId, req.baseUrl());

        var group = groups.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found: " + groupId));

        var conn = connections.findByGroup(group)
                .orElse(JiraConnection.builder().group(group).build());

        applyRequest(conn, req);

        JiraConnection saved = connections.save(conn);
        log.info("[JiraConn] Saved: id={}", saved.getId());
        return mapper.toConnectionDto(saved);
    }

    /**
     * Removes Jira connection for a group.
     */
    @Transactional
    public void removeJiraConnection(Long groupId) {
        log.warn("[JiraConn] Remove: groupId={}", groupId);
        connections.findByGroupId(groupId).ifPresent(connections::delete);
    }

    /**
     * Tests Jira connection.
     */
    public String testJiraConnection(Long groupId) {
        log.info("[JiraConn] Test: groupId={}", groupId);
        JiraConnection conn = getConnectionEntity(groupId);

        boolean success = httpClient.testJiraConnection(conn);
        log.info("[JiraConn] Test {}: groupId={}", success ? "OK" : "FAIL", groupId);

        return success ? "Connected successfully" : "Connection failed";
    }

    private void applyRequest(JiraConnection conn, SaveConnectionRequest req) {
        conn.setBaseUrl(req.baseUrl());
        conn.setEmail(req.email());
        conn.setTokenEncrypted(req.apiToken());
        conn.setDefaultProject(req.defaultProject());
        conn.setDefaultIssueType(req.defaultIssueType());
    }
}
