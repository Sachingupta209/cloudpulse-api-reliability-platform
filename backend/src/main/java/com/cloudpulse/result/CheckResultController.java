package com.cloudpulse.result;

import com.cloudpulse.common.ApiResponse;
import com.cloudpulse.common.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/results")
public class CheckResultController {

    private final CheckResultService checkResultService;

    public CheckResultController(CheckResultService checkResultService) {
        this.checkResultService = checkResultService;
    }

    @GetMapping("/monitor/{monitorId}")
    public ResponseEntity<ApiResponse<PagedResponse<CheckResultDto>>> getResults(
            @PathVariable Long monitorId,
            @RequestParam(required = false) CheckStatus status,
            @PageableDefault(size = 20, sort = "checkedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<CheckResultDto> results = checkResultService.getMonitorCheckResults(monitorId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/monitor/{monitorId}/recent")
    public ResponseEntity<ApiResponse<List<CheckResultDto>>> getRecentResults(
            @PathVariable Long monitorId,
            @RequestParam(defaultValue = "24") int hours) {

        List<CheckResultDto> results = checkResultService.getRecentResultsForChart(monitorId, hours);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
