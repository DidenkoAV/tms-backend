package com.test.system.exceptions.testcase;

/**
 * Import-related errors for test cases.
 */
public class TestCaseImportException extends RuntimeException {
    public TestCaseImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
