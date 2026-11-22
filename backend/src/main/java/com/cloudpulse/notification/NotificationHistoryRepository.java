package com.cloudpulse.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    @EntityGraph(attributePaths = {"monitor", "incident"})
    Page<NotificationHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"monitor", "incident"})
    Page<NotificationHistory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<NotificationHistory> findByIdempotencyKey(String idempotencyKey);

    long countByUserId(Long userId);

    @Modifying
    @Query(value = """
        INSERT INTO notification_history (
            user_id, monitor_id, incident_id, notification_type, recipient, subject,
            html_body, text_body, status, idempotency_key, failure_reason, retry_count,
            max_retries, next_attempt_at, created_at, updated_at
        ) VALUES (
            :userId, :monitorId, :incidentId, :notificationType, :recipient, :subject,
            :htmlBody, :textBody, :status, :idempotencyKey, :failureReason, :retryCount,
            :maxRetries, :nextAttemptAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) ON CONFLICT (idempotency_key) DO NOTHING
        """, nativeQuery = true)
    int insertOnConflictDoNothing(
            @Param("userId") Long userId,
            @Param("monitorId") Long monitorId,
            @Param("incidentId") Long incidentId,
            @Param("notificationType") String notificationType,
            @Param("recipient") String recipient,
            @Param("subject") String subject,
            @Param("htmlBody") String htmlBody,
            @Param("textBody") String textBody,
            @Param("status") String status,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("failureReason") String failureReason,
            @Param("retryCount") int retryCount,
            @Param("maxRetries") int maxRetries,
            @Param("nextAttemptAt") Instant nextAttemptAt
    );

    @Query(value = """
        SELECT id FROM notification_history
        WHERE (status = 'PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP))
           OR (status = 'IN_FLIGHT' AND lease_until <= CURRENT_TIMESTAMP)
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Long> findEligibleNotificationIdsForUpdate(@Param("batchSize") int batchSize);

    @Modifying
    @Query("""
        UPDATE NotificationHistory nh
        SET nh.status = com.cloudpulse.notification.NotificationStatus.IN_FLIGHT,
            nh.claimedBy = :claimedBy,
            nh.claimedAt = :claimedAt,
            nh.leaseUntil = :leaseUntil,
            nh.updatedAt = :now
        WHERE nh.id IN :ids
        """)
    int markClaimedByIds(
            @Param("ids") Collection<Long> ids,
            @Param("claimedBy") String claimedBy,
            @Param("claimedAt") Instant claimedAt,
            @Param("leaseUntil") Instant leaseUntil,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
        UPDATE NotificationHistory nh
        SET nh.status = com.cloudpulse.notification.NotificationStatus.SENT,
            nh.sentAt = :sentAt,
            nh.leaseUntil = null,
            nh.updatedAt = :now
        WHERE nh.id = :id
          AND nh.claimedBy = :claimedBy
          AND nh.status = com.cloudpulse.notification.NotificationStatus.IN_FLIGHT
        """)
    int markSentWithLeaseCheck(
            @Param("id") Long id,
            @Param("claimedBy") String claimedBy,
            @Param("sentAt") Instant sentAt,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
        UPDATE NotificationHistory nh
        SET nh.status = com.cloudpulse.notification.NotificationStatus.PENDING,
            nh.retryCount = nh.retryCount + 1,
            nh.nextAttemptAt = :nextAttemptAt,
            nh.failureReason = :failureReason,
            nh.claimedBy = null,
            nh.leaseUntil = null,
            nh.updatedAt = :now
        WHERE nh.id = :id
          AND nh.claimedBy = :claimedBy
          AND nh.status = com.cloudpulse.notification.NotificationStatus.IN_FLIGHT
        """)
    int markRetryWithLeaseCheck(
            @Param("id") Long id,
            @Param("claimedBy") String claimedBy,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("failureReason") String failureReason,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
        UPDATE NotificationHistory nh
        SET nh.status = com.cloudpulse.notification.NotificationStatus.FAILED,
            nh.retryCount = nh.retryCount + 1,
            nh.failureReason = :failureReason,
            nh.claimedBy = null,
            nh.leaseUntil = null,
            nh.updatedAt = :now
        WHERE nh.id = :id
          AND nh.claimedBy = :claimedBy
          AND nh.status = com.cloudpulse.notification.NotificationStatus.IN_FLIGHT
        """)
    int markFailedWithLeaseCheck(
            @Param("id") Long id,
            @Param("claimedBy") String claimedBy,
            @Param("failureReason") String failureReason,
            @Param("now") Instant now
    );
}
