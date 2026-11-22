package com.cloudpulse.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monitors/{monitorId}/pdf")
    public ResponseEntity<byte[]> getMonitorPdfReport(
            @PathVariable Long monitorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        byte[] pdfBytes = reportService.generateMonitorReport(monitorId, startDate, endDate);
        String filename = String.format("cloudpulse_monitor_%d_%s.pdf", monitorId, FILE_DATE_FMT.format(Instant.now()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfBytes);
    }

    @GetMapping("/summary/pdf")
    public ResponseEntity<byte[]> getSummaryPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        byte[] pdfBytes = reportService.generateSummaryReport(startDate, endDate);
        String filename = String.format("cloudpulse_portfolio_summary_%s.pdf", FILE_DATE_FMT.format(Instant.now()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfBytes);
    }

    @GetMapping("/incidents/pdf")
    public ResponseEntity<byte[]> getIncidentPdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        byte[] pdfBytes = reportService.generateIncidentReport(startDate, endDate);
        String filename = String.format("cloudpulse_incident_log_%s.pdf", FILE_DATE_FMT.format(Instant.now()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfBytes);
    }

    @GetMapping("/performance/pdf")
    public ResponseEntity<byte[]> getPerformancePdfReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        byte[] pdfBytes = reportService.generatePerformanceReport(startDate, endDate);
        String filename = String.format("cloudpulse_performance_sla_%s.pdf", FILE_DATE_FMT.format(Instant.now()));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdfBytes);
    }
}
