package com.cloudpulse.statuspage;

import com.cloudpulse.common.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/status")
public class PublicStatusController {

    private final StatusPageService statusPageService;

    public PublicStatusController(StatusPageService statusPageService) {
        this.statusPageService = statusPageService;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PublicStatusPageDto>> getPublicStatusPage(@PathVariable String slug) {
        PublicStatusPageDto dto = statusPageService.getPublicStatusPage(slug);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .body(ApiResponse.success(dto));
    }
}
