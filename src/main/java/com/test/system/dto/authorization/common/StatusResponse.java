package com.test.system.dto.authorization.common;

public record StatusResponse(String status) {
    public static StatusResponse ok() {
        return new StatusResponse("ok");
    }

    public static StatusResponse sent() {
        return new StatusResponse("sent");
    }
}

