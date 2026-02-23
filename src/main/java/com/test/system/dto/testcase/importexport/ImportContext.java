package com.test.system.dto.testcase.importexport;

import com.test.system.model.cases.TestCase;

import java.util.Map;

/**
 * Context data for test case import operation.
 * Contains all lookup maps needed during import.
 */
public record ImportContext(
        /**
         * Existing test cases grouped by suite ID and title (lowercase).
         * Key format: suiteId -> (titleLowercase -> TestCase)
         */
        Map<String, Map<String, TestCase>> existingCases,

        /**
         * Suite name (lowercase) to suite ID mapping.
         */
        Map<String, Long> suiteByName,

        /**
         * Priority name (lowercase) to priority ID mapping.
         */
        Map<String, Long> priorityByName,

        /**
         * Type name (lowercase) to type ID mapping.
         */
        Map<String, Long> typeByName
) {}

