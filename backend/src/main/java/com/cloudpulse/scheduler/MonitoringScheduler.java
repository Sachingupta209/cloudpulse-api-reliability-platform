package com.cloudpulse.scheduler;

import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.monitor.MonitorStatus;
import com.cloudpulse.monitoring.MonitoringService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final MonitorRepository monitorRepository;
    private final MonitoringService monitoringService;
    private final Executor taskExecutor;

    // Concurrency guard: prevents overlapping check execution for the same monitor
    private final Map<Long, Boolean> inFlightMonitors = new ConcurrentHashMap<>();

    public MonitoringScheduler(
            MonitorRepository monitorRepository,
            MonitoringService monitoringService,
            @Qualifier("monitoringTaskExecutor") Executor taskExecutor) {
        this.monitorRepository = monitorRepository;
        this.monitoringService = monitoringService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedDelayString = "${app.monitoring.scheduler-interval-ms:10000}")
    @SchedulerLock(name = "MonitoringScheduler_scheduleDueChecks", lockAtMostFor = "2m", lockAtLeastFor = "2s")
    public void scheduleDueChecks() {
        List<Monitor> activeMonitors = monitorRepository.findByStatus(MonitorStatus.ACTIVE);
        Instant now = Instant.now();

        for (Monitor monitor : activeMonitors) {
            // Check if due based on checkIntervalSeconds
            boolean isDue = monitor.getLastCheckedAt() == null ||
                    monitor.getLastCheckedAt().plusSeconds(monitor.getCheckIntervalSeconds()).isBefore(now);

            if (isDue) {
                Long monitorId = monitor.getId();

                // Prevent overlapping execution
                if (inFlightMonitors.putIfAbsent(monitorId, Boolean.TRUE) == null) {
                    taskExecutor.execute(() -> {
                        try {
                            monitoringService.executeCheckById(monitorId);
                        } catch (Exception ex) {
                            log.error("Unexpected error executing scheduled check for monitor id={}: {}", monitorId, ex.getMessage(), ex);
                        } finally {
                            inFlightMonitors.remove(monitorId);
                        }
                    });
                } else {
                    log.debug("Skipping check for monitor id={} - execution already in progress", monitorId);
                }
            }
        }
    }
}
