package com.test.system.model.group;

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
     * Whether this is a "personal" group created automatically for a user.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean personal = true;

    /**
     * Creation timestamp (UTC).
     */
    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}

