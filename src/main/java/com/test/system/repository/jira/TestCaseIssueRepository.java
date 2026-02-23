package com.test.system.repository.jira;

import com.test.system.model.cases.TestCase;
import com.test.system.model.jira.TestCaseIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseIssueRepository extends JpaRepository<TestCaseIssue, Long> {

    List<TestCaseIssue> findByTestCaseId(Long testCaseId);
    List<TestCaseIssue> findByTestCaseIn(List<TestCase> testCases);
}
