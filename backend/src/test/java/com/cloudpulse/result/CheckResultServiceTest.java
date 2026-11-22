package com.cloudpulse.result;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.monitor.HttpMethod;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorService;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckResultServiceTest {

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private MonitorService monitorService;

    private CheckResultService checkResultService;
    private User testUser;
    private Monitor testMonitor;

    @BeforeEach
    void setUp() {
        checkResultService = new CheckResultService(checkResultRepository, monitorService);

        testUser = new User("alice@cloudpulse.io", "hashedPass", "Alice", "Smith", Role.ROLE_USER);
        testUser.setId(1L);

        testMonitor = new Monitor(testUser, "Production API", "https://api.example.com", HttpMethod.GET, 60, 5000, 200);
        testMonitor.setId(10L);
    }

    @Test
    @DisplayName("Should retrieve paginated check results for owned monitor")
    void shouldReturnPaginatedResultsWhenOwner() {
        Pageable pageable = PageRequest.of(0, 30);
        CheckResult result1 = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 150L, null, null, "1.2.3.4");
        CheckResult result2 = new CheckResult(testMonitor, CheckStatus.FAILURE, 500, 300L, "Internal Server Error", null, "1.2.3.4");
        Page<CheckResult> page = new PageImpl<>(List.of(result1, result2), pageable, 2);

        when(monitorService.getMonitorEntityForCurrentUser(10L)).thenReturn(testMonitor);
        when(checkResultRepository.findByMonitorIdOrderByCheckedAtDesc(10L, pageable)).thenReturn(page);

        PagedResponse<CheckResultDto> response = checkResultService.getMonitorCheckResults(10L, null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(30);

        verify(monitorService).getMonitorEntityForCurrentUser(10L);
        verify(checkResultRepository).findByMonitorIdOrderByCheckedAtDesc(10L, pageable);
    }

    @Test
    @DisplayName("Should retrieve filtered paginated check results by status")
    void shouldReturnFilteredResultsByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        CheckResult failedResult = new CheckResult(testMonitor, CheckStatus.FAILURE, 503, 1200L, "Service Unavailable", null, "1.2.3.4");
        Page<CheckResult> page = new PageImpl<>(List.of(failedResult), pageable, 1);

        when(monitorService.getMonitorEntityForCurrentUser(10L)).thenReturn(testMonitor);
        when(checkResultRepository.findByMonitorIdAndStatusOrderByCheckedAtDesc(10L, CheckStatus.FAILURE, pageable)).thenReturn(page);

        PagedResponse<CheckResultDto> response = checkResultService.getMonitorCheckResults(10L, CheckStatus.FAILURE, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(CheckStatus.FAILURE);

        verify(monitorService).getMonitorEntityForCurrentUser(10L);
        verify(checkResultRepository).findByMonitorIdAndStatusOrderByCheckedAtDesc(10L, CheckStatus.FAILURE, pageable);
    }

    @Test
    @DisplayName("Tenant Isolation: Should reject results retrieval for monitor belonging to another user")
    void shouldEnforceTenantIsolationOnResultsRetrieval() {
        Pageable pageable = PageRequest.of(0, 30);
        when(monitorService.getMonitorEntityForCurrentUser(999L))
                .thenThrow(new ResourceNotFoundException("Monitor", "id", 999L));

        assertThatThrownBy(() -> checkResultService.getMonitorCheckResults(999L, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Monitor not found with id : '999'");

        verify(checkResultRepository, never()).findByMonitorIdOrderByCheckedAtDesc(anyLong(), any());
        verify(checkResultRepository, never()).findByMonitorIdAndStatusOrderByCheckedAtDesc(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Should retrieve recent results for chart when owner")
    void shouldReturnRecentResultsForChartWhenOwner() {
        CheckResult r1 = new CheckResult(testMonitor, CheckStatus.SUCCESS, 200, 100L, null, null, "1.2.3.4");
        when(monitorService.getMonitorEntityForCurrentUser(10L)).thenReturn(testMonitor);
        when(checkResultRepository.findRecentByMonitorId(eq(10L), any(Instant.class))).thenReturn(List.of(r1));

        List<CheckResultDto> recent = checkResultService.getRecentResultsForChart(10L, 24);

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).getResponseTimeMs()).isEqualTo(100L);
        verify(monitorService).getMonitorEntityForCurrentUser(10L);
    }

    @Test
    @DisplayName("Tenant Isolation: Should reject chart retrieval for another user's monitor")
    void shouldEnforceTenantIsolationOnChartRetrieval() {
        when(monitorService.getMonitorEntityForCurrentUser(999L))
                .thenThrow(new ResourceNotFoundException("Monitor", "id", 999L));

        assertThatThrownBy(() -> checkResultService.getRecentResultsForChart(999L, 24))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(checkResultRepository, never()).findRecentByMonitorId(anyLong(), any());
    }
}
