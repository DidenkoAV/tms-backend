package com.test.system.enums.groups;

public enum InvitationStatus {
    PENDING,    // active, can be accepted or cancelled
    ACCEPTED,   // consumed
    CANCELLED,  // revoked by inviter/owner
    EXPIRED     // auto-marked when TTL passed (optional maintenance)
}
