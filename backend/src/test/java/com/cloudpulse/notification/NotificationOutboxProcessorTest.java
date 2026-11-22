package com.cloudpulse.notification;

import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxProcessorTest {

    @Mock
    private NotificationOutboxClaimService claimService;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private ThreadPoolTaskExecutor notificationTaskExecutor;
    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    private NotificationOutboxProcessor processor;
    private User testUser;
    private NotificationHistory sampleNotification;

    @BeforeEach
    void setUp() {
        processor = new NotificationOutboxProcessor(
                claimService,
                emailService,
                notificationTaskExecutor,
                10,
                90
        );

        testUser = new User("dev@cloudpulse.io", "passHash", "Dev", "User", Role.ROLE_USER);
        testUser.setId(101L);

        sampleNotification = new NotificationHistory(
                testUser, null, null,
                NotificationType.INCIDENT_OPENED, "dev@cloudpulse.io",
                "Incident opened", "<html>body</html>", "text body",
                NotificationStatus.PENDING, "key-1", null, 0
        );
        sampleNotification.setId(42L);
        sampleNotification.setMaxRetries(3);

        lenient().when(notificationTaskExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        lenient().when(threadPoolExecutor.getActiveCount()).thenReturn(0);
        lenient().when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(5);

        // Execute immediately when task is submitted to mock executor
        lenient().doAnswer(inv -> {
            Runnable task = inv.getArgument(0);
            task.run();
            return null;
        }).when(notificationTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    @DisplayName("Should successfully claim and dispatch pending notification")
    void shouldClaimAndDispatchPendingNotification() {
        when(claimService.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(sampleNotification));
        when(emailService.sendSingleEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(true, null));

        processor.processOutbox();

        verify(claimService).claimBatch(eq(5), anyString(), eq(90));
        verify(emailService).sendSingleEmail(eq("dev@cloudpulse.io"), eq("Incident opened"), eq("<html>body</html>"), eq("text body"));
        verify(claimService).completeSuccess(eq(42L), anyString());
    }

    @Test
    @DisplayName("Should schedule retry with exponential backoff on temporary email delivery failure")
    void shouldScheduleRetryOnTemporaryFailure() {
        when(claimService.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(sampleNotification));
        when(emailService.sendSingleEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(false, "Connection refused: localhost:1025"));

        processor.processOutbox();

        verify(claimService).scheduleRetry(eq(42L), anyString(), eq(0), eq("Connection refused: localhost:1025"), eq(20L));
        verify(claimService, never()).markTerminalFailed(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should mark notification as terminal FAILED when max retries are exhausted")
    void shouldMarkTerminalFailedWhenRetriesExhausted() {
        sampleNotification.setRetryCount(2); // Attempt 3 will hit maxRetries = 3
        when(claimService.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(sampleNotification));
        when(emailService.sendSingleEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(false, "Auth failed password=secret123"));

        processor.processOutbox();

        verify(claimService).markTerminalFailed(eq(42L), anyString(), contains("password=secret123"));
        verify(claimService, never()).scheduleRetry(anyLong(), anyString(), anyInt(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should defer claiming when worker executor is at full capacity (backpressure)")
    void shouldDeferClaimingWhenWorkerExecutorAtCapacity() {
        when(threadPoolExecutor.getActiveCount()).thenReturn(5);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(5);

        processor.processOutbox();

        verify(claimService, never()).claimBatch(anyInt(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Worker Crash Simulation: Expired lease allows safe recovery and redelivery")
    void shouldRecoverAndDeliverWhenLeaseExpired() {
        // Simulates an orphaned notification whose worker died while IN_FLIGHT and lease expired
        sampleNotification.setStatus(NotificationStatus.IN_FLIGHT);
        sampleNotification.setLeaseUntil(Instant.now().minusSeconds(10));
        sampleNotification.setClaimedBy("crashed-worker-node-1");

        when(claimService.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(sampleNotification));
        when(emailService.sendSingleEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(true, null));

        processor.processOutbox();

        // Verifies the recovering worker successfully dispatched and marked SENT with its new workerId
        ArgumentCaptor<String> workerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(claimService).completeSuccess(eq(42L), workerIdCaptor.capture());
        assertThat(workerIdCaptor.getValue()).isNotEqualTo("crashed-worker-node-1");
    }

    @Test
    @DisplayName("Lease Ownership Safeguard: Stale worker dropped when lease was reassigned")
    void shouldDropStaleWorkerResultWhenLeaseLost() {
        NotificationHistory staleItem = new NotificationHistory(
                testUser, null, null,
                NotificationType.INCIDENT_OPENED, "dev@cloudpulse.io",
                "Incident opened", "<html>body</html>", "text body",
                NotificationStatus.IN_FLIGHT, "key-stale", null, 0
        );
        staleItem.setId(999L);

        when(claimService.claimBatch(anyInt(), anyString(), anyInt()))
                .thenReturn(List.of(staleItem));
        when(emailService.sendSingleEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new EmailNotificationService.EmailSendResult(true, null));
        // completeSuccess returns false indicating 0 rows updated because claimed_by did not match (reassigned)
        when(claimService.completeSuccess(eq(999L), anyString())).thenReturn(false);

        processor.processOutbox();

        verify(claimService).completeSuccess(eq(999L), anyString());
    }
}
