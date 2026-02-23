package com.test.system.exceptions.results;

public class ResultRunClosedException extends IllegalStateException {
    public ResultRunClosedException(Long runId) {
        super("Run is closed: " + runId);
    }
}
