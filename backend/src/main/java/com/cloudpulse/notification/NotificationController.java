package com.cloudpulse.notification;

import com.cloudpulse.common.ApiResponse;
import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationPreferenceDto preferences = notificationService.getOrCreatePreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDto>> updatePreferences(
            @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationPreferenceDto updated = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated successfully", updated));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationHistoryDto>>> getNotificationHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        PagedResponse<NotificationHistoryDto> history = notificationService.getNotificationHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<NotificationHistoryDto>> sendTestNotification() {
        Long userId = SecurityUtils.getCurrentUserId();
        NotificationHistoryDto result = notificationService.sendTestNotification(userId);
        if (result.status() == NotificationStatus.SENT) {
            return ResponseEntity.ok(ApiResponse.success("Test notification dispatched successfully", result));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Failed to dispatch test notification: " + result.failureReason()));
        }
    }
}
