package com.test.system.exceptions.run;

public class InvalidRunRequestException extends IllegalArgumentException {
    public InvalidRunRequestException(String message) {
        super(message);
    }
}
