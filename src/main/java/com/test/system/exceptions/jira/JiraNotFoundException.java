package com.test.system.exceptions.jira;

public class JiraNotFoundException extends RuntimeException {
    public JiraNotFoundException(String msg) {
        super(msg);
    }
}
