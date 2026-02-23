package com.test.system.exceptions.testcase;

/**
 * 400 Bad Request for invalid test case input (validated by ApiExceptionHandler).
 */
public class TestCaseValidationException extends IllegalArgumentException {
    public TestCaseValidationException(String message) {
        super(message);
    }
}
