package com.cloudpulse.anomaly;

import com.cloudpulse.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;

    public AnomalyController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AnomalyDto>>> getAnomalies(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AnomalyDto> page = anomalyDetectionService.getUserAnomalies(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/monitors/{monitorId}")
    public ResponseEntity<ApiResponse<Page<AnomalyDto>>> getMonitorAnomalies(
            @PathVariable Long monitorId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AnomalyDto> page = anomalyDetectionService.getMonitorAnomalies(monitorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<AnomalyDto>> acknowledgeAnomaly(@PathVariable Long id) {
        AnomalyDto dto = anomalyDetectionService.acknowledgeAnomaly(id);
        return ResponseEntity.ok(ApiResponse.success("Anomaly acknowledged", dto));
    }

    @GetMapping("/unacknowledged-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnacknowledgedCount() {
        long count = anomalyDetectionService.getUnacknowledgedCount();
        return ResponseEntity.ok(ApiResponse.success(Map.of("unacknowledgedCount", count)));
    }
}
