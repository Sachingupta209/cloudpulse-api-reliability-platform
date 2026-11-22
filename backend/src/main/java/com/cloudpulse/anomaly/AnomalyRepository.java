package com.cloudpulse.anomaly;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    @Query("SELECT a FROM Anomaly a JOIN FETCH a.monitor WHERE a.user.id = :userId ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByUserIdWithMonitor(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Anomaly a JOIN FETCH a.monitor WHERE a.monitor.id = :monitorId AND a.user.id = :userId ORDER BY a.detectedAt DESC")
    Page<Anomaly> findByMonitorIdAndUserIdWithMonitor(@Param("monitorId") Long monitorId, @Param("userId") Long userId, Pageable pageable);

    Optional<Anomaly> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndAcknowledgedFalse(Long userId);

    @Query("SELECT COUNT(a) > 0 FROM Anomaly a WHERE a.monitor.id = :monitorId AND a.anomalyType = :type AND a.detectedAt > :since")
    boolean existsRecentByMonitorAndType(@Param("monitorId") Long monitorId, @Param("type") AnomalyType type, @Param("since") Instant since);
}
