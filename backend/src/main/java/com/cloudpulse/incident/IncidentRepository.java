package com.cloudpulse.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByMonitorIdAndStatus(Long monitorId, IncidentStatus status);

    List<Incident> findByUserIdAndStatus(Long userId, IncidentStatus status);

    Optional<Incident> findFirstByMonitorIdAndStatusOrderByStartedAtDesc(Long monitorId, IncidentStatus status);

    Page<Incident> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Page<Incident> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, IncidentStatus status, Pageable pageable);

    List<Incident> findTop5ByUserIdOrderByStartedAtDesc(Long userId);

    long countByUserIdAndStatus(Long userId, IncidentStatus status);

    long countByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.monitor.id IN :monitorIds AND i.startedAt >= :since ORDER BY i.startedAt DESC")
    List<Incident> findRecentByMonitorIdsSince(@org.springframework.data.repository.query.Param("monitorIds") java.util.Collection<Long> monitorIds, @org.springframework.data.repository.query.Param("since") java.time.Instant since, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.monitor.user.id = :userId AND i.startedAt BETWEEN :start AND :end ORDER BY i.startedAt DESC")
    List<Incident> findByUserIdAndStartedAtBetween(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("start") java.time.Instant start, @org.springframework.data.repository.query.Param("end") java.time.Instant end);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Incident i WHERE i.monitor.id = :monitorId AND i.startedAt BETWEEN :start AND :end ORDER BY i.startedAt DESC")
    List<Incident> findByMonitorIdAndStartedAtBetween(@org.springframework.data.repository.query.Param("monitorId") Long monitorId, @org.springframework.data.repository.query.Param("start") java.time.Instant start, @org.springframework.data.repository.query.Param("end") java.time.Instant end);
}
