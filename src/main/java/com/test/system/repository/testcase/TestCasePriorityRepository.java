package com.test.system.repository.testcase;

import com.test.system.model.cases.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCasePriorityRepository extends JpaRepository<Priority, Long> {
}

