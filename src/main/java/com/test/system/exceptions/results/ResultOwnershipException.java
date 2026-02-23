package com.test.system.exceptions.results;

public class ResultOwnershipException extends IllegalArgumentException {
    public ResultOwnershipException(Long runId) {
        super("Result does not belong to run " + runId);
    }

    public ResultOwnershipException(Long runId, String details) {
        super("Some results do not belong to run " + runId + (details != null ? ": " + details : ""));
    }
}
