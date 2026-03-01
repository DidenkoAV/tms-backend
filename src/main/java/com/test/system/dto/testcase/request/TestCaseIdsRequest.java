package com.test.system.dto.testcase.request;

import java.util.List;

public record TestCaseIdsRequest(
        List<Long> caseIds
) {
}
