package com.test.system.repository.run;

import com.test.system.model.status.Status;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for managing test result statuses (PASSED, FAILED, BLOCKED, etc.).
 * Provides basic CRUD operations for status entities.
 */
public interface RunCaseStatusRepository extends JpaRepository<Status, Long> {
    // Uses only standard JpaRepository methods: findAll(), findById(), existsById(), etc.
}

