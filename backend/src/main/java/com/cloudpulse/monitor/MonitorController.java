package com.cloudpulse.monitor;

import com.cloudpulse.common.ApiResponse;
import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.result.CheckResultDto;
import com.cloudpulse.result.CheckResultService;
import com.cloudpulse.result.CheckStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final CheckResultService checkResultService;

    public MonitorController(MonitorService monitorService, CheckResultService checkResultService) {
        this.monitorService = monitorService;
        this.checkResultService = checkResultService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MonitorResponse>> createMonitor(@Valid @RequestBody MonitorRequest request) {
        MonitorResponse response = monitorService.createMonitor(request);
        return new ResponseEntity<>(ApiResponse.success("Monitor created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MonitorResponse>>> getMonitors() {
        List<MonitorResponse> monitors = monitorService.getUserMonitors();
        return ResponseEntity.ok(ApiResponse.success(monitors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorResponse>> getMonitorById(@PathVariable Long id) {
        MonitorResponse response = monitorService.getMonitorById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<PagedResponse<CheckResultDto>>> getMonitorResults(
            @PathVariable Long id,
            @RequestParam(required = false) CheckStatus status,
            @PageableDefault(size = 20, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<CheckResultDto> results = checkResultService.getMonitorCheckResults(id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MonitorResponse>> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request) {
        MonitorResponse response = monitorService.updateMonitor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Monitor updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMonitor(@PathVariable Long id) {
        monitorService.deleteMonitor(id);
        return ResponseEntity.ok(ApiResponse.success("Monitor deleted successfully", null));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<MonitorResponse>> pauseMonitor(@PathVariable Long id) {
        MonitorResponse response = monitorService.pauseMonitor(id);
        return ResponseEntity.ok(ApiResponse.success("Monitor paused successfully", response));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<MonitorResponse>> resumeMonitor(@PathVariable Long id) {
        MonitorResponse response = monitorService.resumeMonitor(id);
        return ResponseEntity.ok(ApiResponse.success("Monitor resumed successfully", response));
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<ApiResponse<CheckResultDto>> triggerCheck(@PathVariable Long id) {
        CheckResultDto result = monitorService.triggerManualCheck(id);
        return ResponseEntity.ok(ApiResponse.success("Health check executed", result));
    }
}
