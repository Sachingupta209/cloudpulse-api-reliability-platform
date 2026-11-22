package com.cloudpulse.incident;

import com.cloudpulse.common.PagedResponse;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.notification.NotificationService;
import com.cloudpulse.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    public IncidentService(IncidentRepository incidentRepository, NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Optional<Incident> handleFailure(Monitor monitor, String failureReason, int consecutiveFailures, int threshold) {
        if (consecutiveFailures >= threshold) {
            // Check if open incident already exists for this monitor
            Optional<Incident> openIncident = incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(
                    monitor.getId(), IncidentStatus.OPEN);

            if (openIncident.isEmpty()) {
                String title = String.format("Downtime detected for '%s'", monitor.getName());
                Incident incident = new Incident(
                        monitor,
                        monitor.getUser(),
                        title,
                        failureReason,
                        Instant.now()
                );
                incident = incidentRepository.save(incident);
                log.warn("Opened new incident id={} for monitor id={} ('{}'): {}",
                        incident.getId(), monitor.getId(), monitor.getName(), failureReason);

                notificationService.notifyIncidentOpened(incident);
                return Optional.of(incident);
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<Incident> handleRecovery(Monitor monitor) {
        Optional<Incident> openIncident = incidentRepository.findFirstByMonitorIdAndStatusOrderByStartedAtDesc(
                monitor.getId(), IncidentStatus.OPEN);

        if (openIncident.isPresent()) {
            Incident incident = openIncident.get();
            incident.resolve(Instant.now());
            incidentRepository.save(incident);
            log.info("Resolved incident id={} for monitor id={} ('{}') after {} seconds downtime",
                    incident.getId(), monitor.getId(), monitor.getName(), incident.getDowntimeSeconds());

            notificationService.notifyIncidentResolved(incident);
            return Optional.of(incident);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public PagedResponse<IncidentDto> getUserIncidents(IncidentStatus status, Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        Page<Incident> page = (status != null)
                ? incidentRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, status, pageable)
                : incidentRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);

        return PagedResponse.of(page.map(IncidentDto::from));
    }

    @Transactional(readOnly = true)
    public List<IncidentDto> getRecentUserIncidents() {
        Long userId = SecurityUtils.getCurrentUserId();
        return incidentRepository.findTop5ByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(IncidentDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDto getIncidentById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", id));

        if (!incident.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Incident", "id", id);
        }

        return IncidentDto.from(incident);
    }
}
