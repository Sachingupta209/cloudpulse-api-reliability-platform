package com.cloudpulse.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class NotificationOutboxClaimService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxClaimService.class);

    private final NotificationHistoryRepository repository;

    public NotificationOutboxClaimService(NotificationHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Short isolated transaction to claim eligible notifications and set them to IN_FLIGHT.
     * Uses FOR UPDATE SKIP LOCKED to prevent duplicate claims, and commits immediately to release locks.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationHistory> claimBatch(int batchSize, String workerId, int leaseSeconds) {
        List<Long> claimedIds = repository.findEligibleNotificationIdsForUpdate(batchSize);
        if (claimedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Instant now = Instant.now();
        Instant leaseUntil = now.plusSeconds(leaseSeconds);
        repository.markClaimedByIds(claimedIds, workerId, now, leaseUntil, now);

        return repository.findAllById(claimedIds);
    }

    /**
     * Short isolated transaction to mark notification as SENT with lease ownership verification.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeSuccess(Long id, String workerId) {
        Instant now = Instant.now();
        int updated = repository.markSentWithLeaseCheck(id, workerId, now, now);
        if (updated == 0) {
            log.warn("Lost lease ownership for notification id={} (claimedBy={}). Stale result dropped.", id, workerId);
            return false;
        }
        log.info("Marked notification id={} as SENT by worker={}", id, workerId);
        return true;
    }

    /**
     * Short isolated transaction to schedule retry with lease ownership verification.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean scheduleRetry(Long id, String workerId, int currentRetries, String sanitizedError, long backoffSeconds) {
        Instant now = Instant.now();
        Instant nextAttempt = now.plusSeconds(backoffSeconds);
        int updated = repository.markRetryWithLeaseCheck(id, workerId, nextAttempt, sanitizedError, now);
        if (updated == 0) {
            log.warn("Lost lease ownership for retry on notification id={} (claimedBy={}). Stale result dropped.", id, workerId);
            return false;
        }
        log.warn("Scheduled retry #{} for notification id={} at {} (reason: {})", currentRetries + 1, id, nextAttempt, sanitizedError);
        return true;
    }

    /**
     * Short isolated transaction to mark notification as terminal FAILED with lease ownership verification.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markTerminalFailed(Long id, String workerId, String sanitizedError) {
        Instant now = Instant.now();
        int updated = repository.markFailedWithLeaseCheck(id, workerId, sanitizedError, now);
        if (updated == 0) {
            log.warn("Lost lease ownership for terminal failure on notification id={} (claimedBy={}). Stale result dropped.", id, workerId);
            return false;
        }
        log.error("Exhausted retries. Marked notification id={} as FAILED (reason: {})", id, sanitizedError);
        return true;
    }
}
