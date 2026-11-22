package com.cloudpulse.report;

import com.cloudpulse.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMonitorPdfReport_Success() throws Exception {
        byte[] pdfContent = "%PDF-1.4 sample content".getBytes();
        when(reportService.generateMonitorReport(eq(1L), any(), any())).thenReturn(pdfContent);

        mockMvc.perform(get("/api/reports/monitors/1/pdf")
                        .param("startDate", "2026-01-01T00:00:00Z")
                        .param("endDate", "2026-01-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(pdfContent));

        verify(reportService).generateMonitorReport(eq(1L), any(), any());
    }

    @Test
    void getSummaryPdfReport_Success() throws Exception {
        byte[] pdfContent = "%PDF-1.4 summary content".getBytes();
        when(reportService.generateSummaryReport(any(), any())).thenReturn(pdfContent);

        mockMvc.perform(get("/api/reports/summary/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(pdfContent));

        verify(reportService).generateSummaryReport(any(), any());
    }

    @Test
    void getIncidentPdfReport_Success() throws Exception {
        byte[] pdfContent = "%PDF-1.4 incident content".getBytes();
        when(reportService.generateIncidentReport(any(), any())).thenReturn(pdfContent);

        mockMvc.perform(get("/api/reports/incidents/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(pdfContent));

        verify(reportService).generateIncidentReport(any(), any());
    }

    @Test
    void getPerformancePdfReport_Success() throws Exception {
        byte[] pdfContent = "%PDF-1.4 perf content".getBytes();
        when(reportService.generatePerformanceReport(any(), any())).thenReturn(pdfContent);

        mockMvc.perform(get("/api/reports/performance/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andExpect(content().bytes(pdfContent));

        verify(reportService).generatePerformanceReport(any(), any());
    }
}
