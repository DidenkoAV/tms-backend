package com.test.system.enums.groups;

/**
 * Type of group.
 * PERSONAL - automatically created for each user, cannot be deleted
 * SHARED - created by users for collaboration, can be deleted by owner
 */
public enum GroupType {
    /**
     * Personal group - created automatically for each user.
     * Cannot be deleted, only the owner is a member.
     */
    PERSONAL,

    /**
     * Shared group - created by users for team collaboration.
     * Can have multiple members, can be deleted by owner.
     */
    SHARED
}

