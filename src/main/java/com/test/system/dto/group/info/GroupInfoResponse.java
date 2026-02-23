package com.test.system.dto.group.info;

import com.test.system.model.group.Group;

public record GroupInfoResponse(
        Long id,
        String name
) {
    public static GroupInfoResponse from(Group g) {
        return new GroupInfoResponse(g.getId(), g.getName());
    }
}

