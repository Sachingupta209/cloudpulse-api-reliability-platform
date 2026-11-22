package com.cloudpulse.anomaly;

import com.cloudpulse.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnomalyControllerTest {

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @InjectMocks
    private AnomalyController anomalyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(anomalyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getAnomalies_Success() throws Exception {
        AnomalyDto dto = new AnomalyDto(
                1L, 10L, "Auth API", "https://api.cloudpulse.io/auth",
                AnomalyType.LATENCY_SPIKE, AnomalySeverity.HIGH,
                650.0, 50.0, "Latency spike", Instant.now(), false, null
        );

        when(anomalyDetectionService.getUserAnomalies(any()))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/anomalies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].anomalyType").value("LATENCY_SPIKE"))
                .andExpect(jsonPath("$.data.content[0].severity").value("HIGH"))
                .andExpect(jsonPath("$.data.content[0].observedValue").value(650.0));

        verify(anomalyDetectionService).getUserAnomalies(any());
    }

    @Test
    void acknowledgeAnomaly_Success() throws Exception {
        AnomalyDto dto = new AnomalyDto(
                1L, 10L, "Auth API", "https://api.cloudpulse.io/auth",
                AnomalyType.LATENCY_SPIKE, AnomalySeverity.HIGH,
                650.0, 50.0, "Latency spike", Instant.now(), true, Instant.now()
        );

        when(anomalyDetectionService.acknowledgeAnomaly(1L)).thenReturn(dto);

        mockMvc.perform(post("/api/anomalies/1/acknowledge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.acknowledged").value(true));

        verify(anomalyDetectionService).acknowledgeAnomaly(1L);
    }

    @Test
    void getUnacknowledgedCount_Success() throws Exception {
        when(anomalyDetectionService.getUnacknowledgedCount()).thenReturn(4L);

        mockMvc.perform(get("/api/anomalies/unacknowledged-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unacknowledgedCount").value(4));

        verify(anomalyDetectionService).getUnacknowledgedCount();
    }
}
