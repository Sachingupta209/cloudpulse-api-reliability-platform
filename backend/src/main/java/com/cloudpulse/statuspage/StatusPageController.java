package com.cloudpulse.statuspage;

import com.cloudpulse.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/status-pages")
public class StatusPageController {

    private final StatusPageService statusPageService;

    public StatusPageController(StatusPageService statusPageService) {
        this.statusPageService = statusPageService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StatusPageResponse>> createStatusPage(@Valid @RequestBody StatusPageRequest request) {
        StatusPageResponse response = statusPageService.createStatusPage(request);
        return new ResponseEntity<>(ApiResponse.success("Status page created successfully", response), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StatusPageResponse>>> getUserStatusPages() {
        List<StatusPageResponse> responses = statusPageService.getUserStatusPages();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StatusPageResponse>> getStatusPageById(@PathVariable Long id) {
        StatusPageResponse response = statusPageService.getStatusPageById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StatusPageResponse>> updateStatusPage(
            @PathVariable Long id,
            @Valid @RequestBody StatusPageRequest request) {
        StatusPageResponse response = statusPageService.updateStatusPage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Status page updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStatusPage(@PathVariable Long id) {
        statusPageService.deleteStatusPage(id);
        return ResponseEntity.ok(ApiResponse.success("Status page deleted successfully", null));
    }
}
