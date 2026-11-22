package com.cloudpulse.result;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CheckResultService {

    private final CheckResultRepository checkResultRepository;
    private final MonitorService monitorService;

    public CheckResultService(CheckResultRepository checkResultRepository, MonitorService monitorService) {
        this.checkResultRepository = checkResultRepository;
        this.monitorService = monitorService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CheckResultDto> getMonitorCheckResults(Long monitorId, CheckStatus status, Pageable pageable) {
        // Enforce user ownership of the monitor
        monitorService.getMonitorEntityForCurrentUser(monitorId);

        Page<CheckResult> page = (status != null)
                ? checkResultRepository.findByMonitorIdAndStatusOrderByCheckedAtDesc(monitorId, status, pageable)
                : checkResultRepository.findByMonitorIdOrderByCheckedAtDesc(monitorId, pageable);

        return PagedResponse.of(page.map(CheckResultDto::from));
    }

    @Transactional(readOnly = true)
    public List<CheckResultDto> getRecentResultsForChart(Long monitorId, int hours) {
        monitorService.getMonitorEntityForCurrentUser(monitorId);
        int validHours = hours > 0 ? Math.min(hours, 168) : 24;
        Instant since = Instant.now().minus(validHours, ChronoUnit.HOURS);

        return checkResultRepository.findRecentByMonitorId(monitorId, since)
                .stream()
                .map(CheckResultDto::from)
                .toList();
    }
}
