// src/main/java/com/test/system/model/group/GroupMembership.java
package com.test.system.model.group;

import com.test.system.enums.groups.GroupRole;
import com.test.system.enums.groups.MembershipStatus;
import com.test.system.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "group_memberships",
        uniqueConstraints = {
                // Optional but useful: prevent duplicate ACTIVE rows per (group,user)
                @UniqueConstraint(name = "uk_group_memberships_group_user", columnNames = {"group_id", "user_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning group. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** Linked user (the member). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Role inside the group: OWNER or MEMBER. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupRole role;

    /** Lifecycle status of the membership (ACTIVE or REMOVED). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    /** Who invited this user (null for the initial owner). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    /** Creation/Join timestamp (UTC). */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
