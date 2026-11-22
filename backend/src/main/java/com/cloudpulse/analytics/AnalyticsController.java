package com.cloudpulse.analytics;

import com.cloudpulse.common.ApiResponse;
import com.cloudpulse.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary() {
        DashboardSummaryDto summary = analyticsService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/advanced")
    public ResponseEntity<ApiResponse<AdvancedAnalyticsDto>> getAdvancedAnalytics() {
        AdvancedAnalyticsDto advanced = analyticsService.getAdvancedAnalytics();
        return ResponseEntity.ok(ApiResponse.success(advanced));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<ResponseTimeTrendDto>>> getTrends(@RequestParam(defaultValue = "24") int hours) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<ResponseTimeTrendDto> trends = analyticsService.calculateResponseTimeTrends(userId, hours);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }
}
