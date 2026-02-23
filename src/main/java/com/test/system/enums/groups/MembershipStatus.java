package com.test.system.enums.groups;

public enum MembershipStatus {
    ACTIVE,
    PENDING,
    REMOVED;

    public boolean isActive() {
        return this == ACTIVE;
    }
}
