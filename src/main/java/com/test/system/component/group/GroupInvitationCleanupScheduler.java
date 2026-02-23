package com.test.system.component.group;

import com.test.system.enums.groups.InvitationStatus;
import com.test.system.repository.group.GroupInvitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled task for cleaning up expired group invitations.
 * Runs periodically to remove PENDING invitations that have expired.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupInvitationCleanupScheduler {

    private static final String LOG_PREFIX = "[GroupInviteCleanup]";

    private final GroupInvitationRepository invitations;

    /**
     * Prunes expired PENDING invitations on a schedule.
     * Cron expression is configured via `app.invites.cleanup.cron` (default: every hour).
     * Timezone is configured via `spring.task.scheduling.time-zone` (default: UTC).
     */
    @Scheduled(
            cron = "${app.invites.cleanup.cron:0 0 * * * *}",
            zone = "${spring.task.scheduling.time-zone:UTC}"
    )
    @Transactional
    public void cleanUpExpiredInvitations() {
        log.debug("{} starting cleanup of expired invitations", LOG_PREFIX);

        int pruned = invitations.deleteExpiredInvitations(
                InvitationStatus.PENDING,
                Instant.now()
        );

        if (pruned > 0) {
            log.info("{} pruned {} expired PENDING invitations", LOG_PREFIX, pruned);
        } else {
            log.debug("{} no expired invitations to prune", LOG_PREFIX);
        }
    }
}

