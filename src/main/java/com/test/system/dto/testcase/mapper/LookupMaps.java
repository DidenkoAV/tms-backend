package com.test.system.dto.testcase.mapper;

import com.test.system.model.user.User;

import java.util.Map;

/**
 * Container for pre-loaded lookup data to avoid N+1 queries in mapper.
 * All maps are guaranteed to be non-null (can be empty).
 */
public record LookupMaps(
        Map<Long, String> suiteNamesById,
        Map<Long, String> typeNamesById,
        Map<Long, String> priorityNamesById,
        Map<Long, User> authorsById
) {
    /**
     * Empty lookup maps (for cases when no lookups are needed).
     */
    public static final LookupMaps EMPTY = new LookupMaps(
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of()
    );

    /**
     * Get suite name by ID, or null if not found.
     */
    public String getSuiteName(Long suiteId) {
        return suiteId == null ? null : suiteNamesById.get(suiteId);
    }

    /**
     * Get type name by ID, or null if not found.
     */
    public String getTypeName(Long typeId) {
        return typeId == null ? null : typeNamesById.get(typeId);
    }

    /**
     * Get priority name by ID, or null if not found.
     */
    public String getPriorityName(Long priorityId) {
        return priorityId == null ? null : priorityNamesById.get(priorityId);
    }

    /**
     * Get author by ID, or null if not found.
     */
    public User getAuthor(Long authorId) {
        return authorId == null ? null : authorsById.get(authorId);
    }
}

