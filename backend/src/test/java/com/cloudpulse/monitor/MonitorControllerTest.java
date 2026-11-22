package com.cloudpulse.monitor;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.exception.GlobalExceptionHandler;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.result.CheckResultDto;
import com.cloudpulse.result.CheckResultService;
import com.cloudpulse.result.CheckStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MonitorControllerTest {

    @Mock
    private MonitorService monitorService;

    @Mock
    private CheckResultService checkResultService;

    @InjectMocks
    private MonitorController monitorController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(monitorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("GET /api/monitors/{id} should return monitor details with HTTP 200")
    void shouldReturnMonitorDetails() throws Exception {
        MonitorResponse response = new MonitorResponse(
                42L, "Billing Service", "https://api.example.com/billing", HttpMethod.GET,
                60, 5000, 200, null, null, MonitorStatus.ACTIVE, HealthStatus.HEALTHY,
                0, Instant.now(), Instant.now(), Instant.now(), List.of(), 99.8, 115.0
        );

        when(monitorService.getMonitorById(42L)).thenReturn(response);

        mockMvc.perform(get("/api/monitors/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.name").value("Billing Service"))
                .andExpect(jsonPath("$.data.uptimePercentage").value(99.8))
                .andExpect(jsonPath("$.data.avgResponseTimeMs").value(115.0));

        verify(monitorService).getMonitorById(42L);
    }

    @Test
    @DisplayName("GET /api/monitors/{id} should return HTTP 404 when monitor does not belong to user")
    void shouldReturn404WhenMonitorNotFoundOrNotOwned() throws Exception {
        when(monitorService.getMonitorById(999L))
                .thenThrow(new ResourceNotFoundException("Monitor", "id", 999L));

        mockMvc.perform(get("/api/monitors/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Monitor not found with id : '999'"));

        verify(monitorService).getMonitorById(999L);
    }

    @Test
    @DisplayName("GET /api/monitors/{id}/results should return paginated check history with HTTP 200")
    void shouldReturnPaginatedCheckResults() throws Exception {
        CheckResultDto dto1 = new CheckResultDto(1L, 42L, CheckStatus.SUCCESS, 200, 110L, null, null, Instant.now(), "1.2.3.4");
        CheckResultDto dto2 = new CheckResultDto(2L, 42L, CheckStatus.FAILURE, 500, 250L, "Server error", null, Instant.now(), "1.2.3.4");
        PagedResponse<CheckResultDto> paged = new PagedResponse<>(List.of(dto1, dto2), 0, 30, 2, 1, true);

        when(checkResultService.getMonitorCheckResults(eq(42L), any(), any()))
                .thenReturn(paged);

        mockMvc.perform(get("/api/monitors/42/results?page=0&size=30").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[1].id").value(2))
                .andExpect(jsonPath("$.data.content[1].status").value("FAILURE"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(30));

        verify(checkResultService).getMonitorCheckResults(eq(42L), any(), any());
    }

    @Test
    @DisplayName("GET /api/monitors/{id}/results should enforce tenant isolation and return HTTP 404")
    void shouldReturn404OnResultsWhenNotOwner() throws Exception {
        when(checkResultService.getMonitorCheckResults(eq(999L), any(), any()))
                .thenThrow(new ResourceNotFoundException("Monitor", "id", 999L));

        mockMvc.perform(get("/api/monitors/999/results?page=0&size=30").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Monitor not found with id : '999'"));

        verify(checkResultService).getMonitorCheckResults(eq(999L), any(), any());
    }

    @Test
    @DisplayName("GET /api/monitors should return list of monitors with HTTP 200")
    void shouldReturnMonitorsList() throws Exception {
        MonitorResponse response = new MonitorResponse(
                1L, "Auth Service", "https://api.example.com/auth", HttpMethod.GET,
                60, 5000, 200, null, null, MonitorStatus.ACTIVE, HealthStatus.HEALTHY,
                0, Instant.now(), Instant.now(), Instant.now(), List.of(), 100.0, 95.0
        );

        when(monitorService.getUserMonitors()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/monitors").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Auth Service"));

        verify(monitorService).getUserMonitors();
    }
}
