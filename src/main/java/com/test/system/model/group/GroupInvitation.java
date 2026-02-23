package com.test.system.model.group;

import com.test.system.enums.groups.InvitationStatus;
import com.test.system.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An invitation to join a group, addressed to a specific email.
 * Stores only the hash of the token for security (raw token is sent via email).
 */
@Entity
@Table(
        name = "group_invitations",
        uniqueConstraints = {
                // Ensure there is at most one PENDING invite per (group, email)
                @UniqueConstraint(name = "uk_invites_group_email_pending", columnNames = {"group_id", "invitee_email", "status"})
        },
        indexes = {
                @Index(name = "ix_invites_group_id", columnList = "group_id"),
                @Index(name = "ix_invites_invitee_email", columnList = "invitee_email"),
                @Index(name = "ix_invites_status", columnList = "status"),
                @Index(name = "ix_invites_token_hash", columnList = "token_hash")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning group. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    /** Who created the invitation (must be the group OWNER in our minimal model). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    /** Target email (normalized lower-case). */
    @Column(name = "invitee_email", nullable = false, length = 320)
    private String inviteeEmail;

    /** Optional reference to a user account that accepted the invite. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_user_id")
    private User inviteeUser;

    /** SHA-256 hex of the raw token; raw token is never stored. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** Current status of the invitation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    /** Created time (UTC). */
    @Builder.Default
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /** Last email send time (UTC) — used for resends/backoff. */
    @Builder.Default
    @Column(nullable = false)
    private Instant lastSentAt = Instant.now();

    /** Expiration time (UTC). */
    @Column(nullable = false)
    private Instant expiresAt;

    /** When accepted/cancelled (UTC). */
    private Instant acceptedAt;
    private Instant cancelledAt;
}
