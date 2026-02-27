package com.test.system.dto.group.invitation;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of accepting a group invitation.
 * Contains information needed for frontend to decide next steps.
 */
@Getter
@Builder
public class InviteAcceptResult {
    
    /**
     * Whether the user needs to set a password (was a placeholder user).
     */
    private final boolean needsPassword;
    
    /**
     * Email of the user who accepted the invitation.
     */
    private final String email;
    
    /**
     * Name of the group the user was invited to.
     */
    private final String groupName;
    
    /**
     * ID of the group.
     */
    private final Long groupId;
}

