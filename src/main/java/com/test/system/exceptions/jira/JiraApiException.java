package com.test.system.exceptions.jira;

public class JiraApiException extends RuntimeException {
    public JiraApiException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
