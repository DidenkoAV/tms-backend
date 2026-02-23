package com.test.system.exceptions.run;

import com.test.system.exceptions.common.NotFoundException;

public class RunCaseNotInRunException extends NotFoundException {
    public RunCaseNotInRunException(String message) {
        super(message);
    }
}
