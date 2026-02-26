package com.test.system.model.group;

import com.test.system.enums.groups.GroupType;
import com.test.system.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A collaborative container that owns members and their permissions.
 * We keep it minimal: one owner per group, a human-friendly name, and timestamps.
 * <p>
 * Table name is "groups" (plural) to avoid clashing with SQL reserved words.
 */
@Entity
@Table(
        name = "groups",
        indexes = {
                @Index(name = "ix_groups_owner_id", columnList = "owner_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-friendly display name (e.g., "Personal group for user@example.com").
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Group owner. There must be exactly one OWNER membership per group.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Type of group: PERSONAL (auto-created, cannot be deleted) or SHARED (user-created, can be deleted).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 20)
    private GroupType groupType = GroupType.PERSONAL;

    /**
     * Creation timestamp (UTC).
     */
    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Last update timestamp (UTC).
     */
    @Column(nullable = true)
    private Instant updatedAt;

    /**
     * Helper method to check if this is a personal group.
     */
    public boolean isPersonal() {
        return groupType == GroupType.PERSONAL;
    }

    /**
     * Helper method to check if this is a shared group.
     */
    public boolean isShared() {
        return groupType == GroupType.SHARED;
    }
}

