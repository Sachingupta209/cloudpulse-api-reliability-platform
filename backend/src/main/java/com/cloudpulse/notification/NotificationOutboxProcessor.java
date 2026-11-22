package com.cloudpulse.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class NotificationOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxProcessor.class);

    private final NotificationOutboxClaimService claimService;
    private final EmailNotificationService emailService;
    private final ThreadPoolTaskExecutor notificationTaskExecutor;
    private final int configuredBatchSize;
    private final int leaseSeconds;
    private final String nodeIdentifier;

    public NotificationOutboxProcessor(
            NotificationOutboxClaimService claimService,
            EmailNotificationService emailService,
            @Qualifier("notificationTaskExecutor") ThreadPoolTaskExecutor notificationTaskExecutor,
            @Value("${app.notifications.batch-size:10}") int configuredBatchSize,
            @Value("${app.notifications.lease-seconds:90}") int leaseSeconds) {
        this.claimService = claimService;
        this.emailService = emailService;
        this.notificationTaskExecutor = notificationTaskExecutor;
        this.configuredBatchSize = Math.max(1, configuredBatchSize);
        this.leaseSeconds = Math.max(15, leaseSeconds);
        this.nodeIdentifier = resolveNodeIdentifier();
    }

    private static String resolveNodeIdentifier() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "node-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    @Scheduled(fixedDelayString = "${app.notifications.poll-delay-ms:5000}")
    public void processOutbox() {
        int availableSlots = calculateAvailableSlots();
        if (availableSlots <= 0) {
            log.debug("Notification worker pool is saturated. Deferring claim.");
            return;
        }

        int batchSize = Math.min(availableSlots, configuredBatchSize);
        String workerId = nodeIdentifier + ":" + UUID.randomUUID().toString().substring(0, 8);

        // Phase 1: Atomic short-transaction claim (releases DB lock immediately upon commit)
        List<NotificationHistory> claimedBatch;
        try {
            claimedBatch = claimService.claimBatch(batchSize, workerId, leaseSeconds);
        } catch (Exception e) {
            log.error("Failed to claim notification batch: {}", e.getMessage());
            return;
        }

        if (claimedBatch.isEmpty()) {
            return;
        }

        log.debug("Worker '{}' claimed {} notifications for processing", workerId, claimedBatch.size());

        // Phase 2: Asynchronous dispatch (network I/O outside DB transactions)
        for (NotificationHistory notification : claimedBatch) {
            notificationTaskExecutor.execute(() -> dispatchNotification(notification, workerId));
        }
    }

    private void dispatchNotification(NotificationHistory notification, String workerId) {
        try {
            EmailNotificationService.EmailSendResult sendResult = emailService.sendSingleEmail(
                    notification.getRecipient(),
                    notification.getSubject(),
                    notification.getHtmlBody(),
                    notification.getTextBody()
            );

            // Phase 3: Short transaction to persist outcome with lease ownership check
            if (sendResult.success()) {
                claimService.completeSuccess(notification.getId(), workerId);
            } else {
                int nextRetry = notification.getRetryCount() + 1;
                if (nextRetry >= notification.getMaxRetries()) {
                    claimService.markTerminalFailed(notification.getId(), workerId, sendResult.errorMessage());
                } else {
                    long backoffSec = (long) Math.pow(2, nextRetry) * 10;
                    claimService.scheduleRetry(notification.getId(), workerId, notification.getRetryCount(), sendResult.errorMessage(), backoffSec);
                }
            }
        } catch (Exception e) {
            String sanitizedError = NotificationSanitizer.sanitizeErrorMessage(e);
            log.error("Unexpected error in worker dispatch for notification id={}: {}", notification.getId(), sanitizedError);
            int nextRetry = notification.getRetryCount() + 1;
            if (nextRetry >= notification.getMaxRetries()) {
                claimService.markTerminalFailed(notification.getId(), workerId, sanitizedError);
            } else {
                long backoffSec = (long) Math.pow(2, nextRetry) * 10;
                claimService.scheduleRetry(notification.getId(), workerId, notification.getRetryCount(), sanitizedError, backoffSec);
            }
        }
    }

    private int calculateAvailableSlots() {
        ThreadPoolExecutor pool = notificationTaskExecutor.getThreadPoolExecutor();
        int active = pool.getActiveCount();
        int max = pool.getMaximumPoolSize();
        return Math.max(0, max - active);
    }
}
