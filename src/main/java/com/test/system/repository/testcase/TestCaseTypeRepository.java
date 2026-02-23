package com.test.system.repository.testcase;

import com.test.system.model.cases.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestCaseTypeRepository extends JpaRepository<CaseType, Long> {
}

