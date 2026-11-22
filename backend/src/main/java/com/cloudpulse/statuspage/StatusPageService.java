package com.cloudpulse.statuspage;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.ResourceNotFoundException;
import com.cloudpulse.incident.Incident;
import com.cloudpulse.incident.IncidentRepository;
import com.cloudpulse.incident.IncidentStatus;
import com.cloudpulse.monitor.HealthStatus;
import com.cloudpulse.monitor.Monitor;
import com.cloudpulse.monitor.MonitorRepository;
import com.cloudpulse.monitor.MonitorStatus;
import com.cloudpulse.notification.NotificationSanitizer;
import com.cloudpulse.result.CheckResultRepository;
import com.cloudpulse.security.SecurityUtils;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StatusPageService {

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "api", "auth", "admin", "login", "register", "status", "dashboard",
            "settings", "public", "monitors", "incidents", "analytics", "notifications"
    );

    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$");
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private final StatusPageRepository statusPageRepository;
    private final StatusPageMonitorRepository statusPageMonitorRepository;
    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public StatusPageService(
            StatusPageRepository statusPageRepository,
            StatusPageMonitorRepository statusPageMonitorRepository,
            MonitorRepository monitorRepository,
            CheckResultRepository checkResultRepository,
            IncidentRepository incidentRepository,
            UserRepository userRepository) {
        this.statusPageRepository = statusPageRepository;
        this.statusPageMonitorRepository = statusPageMonitorRepository;
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StatusPageResponse createStatusPage(StatusPageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String slug = resolveSlugForCreate(request.getSlug());
        boolean isPublished = Boolean.TRUE.equals(request.getIsPublished()); // defaults to false if null

        StatusPage statusPage = new StatusPage(user, request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                slug, isPublished);

        StatusPage saved = statusPageRepository.save(statusPage);

        if (request.getMonitorIds() != null && !request.getMonitorIds().isEmpty()) {
            attachMonitors(saved, request.getMonitorIds(), userId);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StatusPageResponse> getUserStatusPages() {
        Long userId = SecurityUtils.getCurrentUserId();
        return statusPageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatusPageResponse getStatusPageById(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        StatusPage statusPage = statusPageRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found with id: " + id));
        return toResponse(statusPage);
    }

    @Transactional
    public StatusPageResponse updateStatusPage(Long id, StatusPageRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        StatusPage statusPage = statusPageRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found with id: " + id));

        statusPage.setTitle(request.getTitle().trim());
        statusPage.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String cleanSlug = request.getSlug().trim().toLowerCase();
            validateCustomSlug(cleanSlug);
            if (statusPageRepository.existsBySlugAndIdNot(cleanSlug, id)) {
                throw new BadRequestException("Slug is already in use by another status page: " + cleanSlug);
            }
            statusPage.setSlug(cleanSlug);
        }

        if (request.getIsPublished() != null) {
            statusPage.setPublished(request.getIsPublished());
        }

        // Atomically replace monitors mapping
        statusPageMonitorRepository.deleteByStatusPageId(statusPage.getId());
        statusPage.getMonitors().clear();

        if (request.getMonitorIds() != null && !request.getMonitorIds().isEmpty()) {
            attachMonitors(statusPage, request.getMonitorIds(), userId);
        }

        StatusPage updated = statusPageRepository.save(statusPage);
        return toResponse(updated);
    }

    @Transactional
    public void deleteStatusPage(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        StatusPage statusPage = statusPageRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found with id: " + id));
        statusPageRepository.delete(statusPage);
    }

    @Transactional(readOnly = true)
    public PublicStatusPageDto getPublicStatusPage(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResourceNotFoundException("Status page not found");
        }

        StatusPage statusPage = statusPageRepository.findBySlug(slug.trim().toLowerCase())
                .filter(StatusPage::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found with slug: " + slug));

        List<StatusPageMonitor> spMonitors = statusPageMonitorRepository
                .findByStatusPageIdOrderByDisplayOrderAsc(statusPage.getId());

        if (spMonitors.isEmpty()) {
            return new PublicStatusPageDto(
                    statusPage.getTitle(),
                    statusPage.getDescription(),
                    PublicSystemStatus.NO_DATA,
                    statusPage.getUpdatedAt(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        List<Long> monitorIds = spMonitors.stream()
                .map(spm -> spm.getMonitor().getId())
                .toList();

        // 1. Batch aggregate uptime & response time (24h, 7d, 30d)
        Instant now = Instant.now();
        Instant since24h = now.minus(24, ChronoUnit.HOURS);
        Instant since7d = now.minus(7, ChronoUnit.DAYS);
        Instant since30d = now.minus(30, ChronoUnit.DAYS);

        Map<Long, MetricBucket> metrics24h = queryMetricBuckets(monitorIds, since24h);
        Map<Long, MetricBucket> metrics7d = queryMetricBuckets(monitorIds, since7d);
        Map<Long, MetricBucket> metrics30d = queryMetricBuckets(monitorIds, since30d);

        // 2. Map public monitors
        List<PublicMonitorDto> publicMonitors = new ArrayList<>();
        int pausedCount = 0;
        int activeCount = 0;
        int healthyCount = 0;
        int unhealthyCount = 0;
        long totalObservations = 0;

        for (StatusPageMonitor spm : spMonitors) {
            Monitor m = spm.getMonitor();
            Long mid = m.getId();

            MetricBucket b24 = metrics24h.get(mid);
            MetricBucket b7 = metrics7d.get(mid);
            MetricBucket b30 = metrics30d.get(mid);

            Double u24 = calculateUptime(b24);
            Double u7 = calculateUptime(b7);
            Double u30 = calculateUptime(b30);
            Double avgLatency = (b24 != null && b24.avgLatency != null)
                    ? Math.round(b24.avgLatency * 100.0) / 100.0
                    : null;

            if (b24 != null) {
                totalObservations += b24.totalChecks;
            }

            String publicStatus;
            if (m.getStatus() == MonitorStatus.PAUSED) {
                publicStatus = "PAUSED";
                pausedCount++;
            } else {
                activeCount++;
                if (m.getHealthStatus() == HealthStatus.HEALTHY) {
                    publicStatus = "OPERATIONAL";
                    healthyCount++;
                } else if (m.getHealthStatus() == HealthStatus.UNHEALTHY) {
                    publicStatus = "DOWN";
                    unhealthyCount++;
                } else {
                    publicStatus = "UNKNOWN";
                }
            }

            publicMonitors.add(new PublicMonitorDto(
                    m.getName(),
                    publicStatus,
                    u24,
                    u7,
                    u30,
                    avgLatency
            ));
        }

        // 3. Determine Overall System Status
        PublicSystemStatus overallStatus;
        if (spMonitors.isEmpty()) {
            overallStatus = PublicSystemStatus.NO_DATA;
        } else if (activeCount == 0 && pausedCount > 0) {
            overallStatus = PublicSystemStatus.MAINTENANCE;
        } else if (totalObservations == 0 && healthyCount == 0 && unhealthyCount == 0) {
            overallStatus = PublicSystemStatus.NO_DATA;
        } else if (activeCount > 0 && unhealthyCount == activeCount) {
            overallStatus = PublicSystemStatus.OUTAGE;
        } else if (unhealthyCount > 0) {
            overallStatus = PublicSystemStatus.DEGRADED;
        } else if (healthyCount > 0) {
            overallStatus = PublicSystemStatus.OPERATIONAL;
        } else {
            overallStatus = PublicSystemStatus.NO_DATA;
        }

        // 4. Fetch 30-day recent incidents (bounded to 10 max)
        List<Incident> incidents = incidentRepository.findRecentByMonitorIdsSince(
                monitorIds, since30d, PageRequest.of(0, 10));

        List<PublicIncidentDto> publicIncidents = incidents.stream()
                .map(inc -> {
                    Long durationMinutes = null;
                    if (inc.getStatus() == IncidentStatus.RESOLVED && inc.getResolvedAt() != null) {
                        durationMinutes = Duration.between(inc.getStartedAt(), inc.getResolvedAt()).toMinutes();
                    } else if (inc.getStatus() == IncidentStatus.OPEN) {
                        durationMinutes = Duration.between(inc.getStartedAt(), now).toMinutes();
                    }
                    String sanitizedTitle = NotificationSanitizer.sanitizeErrorMessage(inc.getTitle());
                    return new PublicIncidentDto(
                            inc.getMonitor().getName(),
                            sanitizedTitle,
                            inc.getStatus().name(),
                            inc.getStartedAt(),
                            inc.getResolvedAt(),
                            durationMinutes
                    );
                })
                .toList();

        return new PublicStatusPageDto(
                statusPage.getTitle(),
                statusPage.getDescription(),
                overallStatus,
                statusPage.getUpdatedAt(),
                publicMonitors,
                publicIncidents
        );
    }

    private void attachMonitors(StatusPage statusPage, List<Long> monitorIds, Long userId) {
        List<Monitor> userMonitors = monitorRepository.findAllById(monitorIds);
        Map<Long, Monitor> monitorMap = userMonitors.stream()
                .collect(Collectors.toMap(Monitor::getId, m -> m));

        for (Long mid : monitorIds) {
            Monitor m = monitorMap.get(mid);
            if (m == null || !m.getUser().getId().equals(userId)) {
                throw new BadRequestException("One or more selected monitors do not belong to the authenticated user");
            }
        }

        int order = 0;
        for (Long mid : monitorIds) {
            Monitor m = monitorMap.get(mid);
            StatusPageMonitor spm = new StatusPageMonitor(statusPage, m, order++);
            statusPage.getMonitors().add(spm);
            statusPageMonitorRepository.save(spm);
        }
    }

    private String resolveSlugForCreate(String customSlug) {
        if (customSlug != null && !customSlug.isBlank()) {
            String cleanSlug = customSlug.trim().toLowerCase();
            validateCustomSlug(cleanSlug);
            if (statusPageRepository.existsBySlug(cleanSlug)) {
                throw new BadRequestException("Slug is already in use: " + cleanSlug);
            }
            return cleanSlug;
        }

        // Auto-generate collision-safe slug with retry
        for (int i = 0; i < 5; i++) {
            String candidate = generateRandomSlug();
            if (!statusPageRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to generate a unique slug after 5 attempts");
    }

    private void validateCustomSlug(String slug) {
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new BadRequestException("Slug must be lowercase alphanumeric and between 3 and 50 characters: " + slug);
        }
        if (RESERVED_SLUGS.contains(slug)) {
            throw new BadRequestException("Slug is a reserved system keyword: " + slug);
        }
    }

    public String generateRandomSlug() {
        char[] token = new char[16];
        byte[] bytes = new byte[8];
        secureRandom.nextBytes(bytes);
        for (int i = 0; i < 8; i++) {
            token[i * 2] = HEX_CHARS[(bytes[i] >> 4) & 0x0F];
            token[i * 2 + 1] = HEX_CHARS[bytes[i] & 0x0F];
        }
        return "sp-" + new String(token);
    }

    private Map<Long, MetricBucket> queryMetricBuckets(Collection<Long> monitorIds, Instant since) {
        List<Object[]> rows = checkResultRepository.aggregateStatsByMonitorIdsSince(monitorIds, since);
        Map<Long, MetricBucket> result = new HashMap<>();
        for (Object[] row : rows) {
            Long mid = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long success = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            Double avgLatency = row[3] != null ? ((Number) row[3]).doubleValue() : null;
            result.put(mid, new MetricBucket(total, success, avgLatency));
        }
        return result;
    }

    private Double calculateUptime(MetricBucket bucket) {
        if (bucket == null || bucket.totalChecks == 0) {
            return null; // Return null so UI shows "No data" instead of inaccurate 100%
        }
        double uptime = ((double) bucket.successChecks / bucket.totalChecks) * 100.0;
        return Math.round(uptime * 100.0) / 100.0;
    }

    private StatusPageResponse toResponse(StatusPage sp) {
        List<StatusPageMonitor> spMonitors = statusPageMonitorRepository
                .findByStatusPageIdOrderByDisplayOrderAsc(sp.getId());

        List<StatusPageResponse.StatusPageMonitorDto> monitorDtos = spMonitors.stream()
                .map(spm -> new StatusPageResponse.StatusPageMonitorDto(
                        spm.getMonitor().getId(),
                        spm.getMonitor().getName(),
                        spm.getMonitor().getStatus().name(),
                        spm.getMonitor().getHealthStatus().name(),
                        spm.getDisplayOrder()
                ))
                .toList();

        return new StatusPageResponse(
                sp.getId(),
                sp.getTitle(),
                sp.getDescription(),
                sp.getSlug(),
                sp.isPublished(),
                monitorDtos,
                sp.getCreatedAt(),
                sp.getUpdatedAt()
        );
    }

    private static class MetricBucket {
        final long totalChecks;
        final long successChecks;
        final Double avgLatency;

        MetricBucket(long totalChecks, long successChecks, Double avgLatency) {
            this.totalChecks = totalChecks;
            this.successChecks = successChecks;
            this.avgLatency = avgLatency;
        }
    }
}
