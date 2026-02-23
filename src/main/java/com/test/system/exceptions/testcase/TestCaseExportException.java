package com.test.system.exceptions.testcase;

/**
 * Export-related errors for test cases.
 */
public class TestCaseExportException extends RuntimeException {
    public TestCaseExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
