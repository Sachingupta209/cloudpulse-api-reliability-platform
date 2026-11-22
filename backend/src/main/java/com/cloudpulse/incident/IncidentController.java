package com.cloudpulse.incident;

import com.cloudpulse.common.ApiResponse;
import com.cloudpulse.common.PagedResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<IncidentDto>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<IncidentDto> incidents = incidentService.getUserIncidents(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(incidents));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<IncidentDto>>> getRecentIncidents() {
        List<IncidentDto> recent = incidentService.getRecentUserIncidents();
        return ResponseEntity.ok(ApiResponse.success(recent));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IncidentDto>> getIncidentById(@PathVariable Long id) {
        IncidentDto incident = incidentService.getIncidentById(id);
        return ResponseEntity.ok(ApiResponse.success(incident));
    }
}
