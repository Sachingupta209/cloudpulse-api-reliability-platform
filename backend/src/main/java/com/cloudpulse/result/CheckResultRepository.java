package com.cloudpulse.result;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    Page<CheckResult> findByMonitorIdOrderByCheckedAtDesc(Long monitorId, Pageable pageable);

    Page<CheckResult> findByMonitorIdAndStatusOrderByCheckedAtDesc(Long monitorId, CheckStatus status, Pageable pageable);

    @Query("SELECT r FROM CheckResult r WHERE r.monitor.id = :monitorId AND r.checkedAt >= :since ORDER BY r.checkedAt ASC")
    List<CheckResult> findRecentByMonitorId(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Query("SELECT r FROM CheckResult r WHERE r.monitor.id = :monitorId AND r.checkedAt BETWEEN :start AND :end ORDER BY r.checkedAt ASC")
    List<CheckResult> findByMonitorIdAndCheckedAtBetween(@Param("monitorId") Long monitorId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(r) FROM CheckResult r WHERE r.monitor.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM CheckResult r WHERE r.monitor.user.id = :userId AND r.status = 'FAILURE'")
    long countFailuresByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.responseTimeMs) FROM CheckResult r WHERE r.monitor.user.id = :userId AND r.status = 'SUCCESS'")
    Double findAverageResponseTimeByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.responseTimeMs) FROM CheckResult r WHERE r.monitor.id = :monitorId AND r.status = 'SUCCESS' AND r.checkedAt >= :since")
    Double findAverageResponseTimeByMonitorIdSince(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM CheckResult r WHERE r.monitor.id = :monitorId AND r.checkedAt >= :since")
    long countByMonitorIdSince(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM CheckResult r WHERE r.monitor.id = :monitorId AND r.status = 'SUCCESS' AND r.checkedAt >= :since")
    long countSuccessfulByMonitorIdSince(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Query("SELECT r.monitor.id, " +
           "COUNT(r), " +
           "SUM(CASE WHEN r.status = 'SUCCESS' THEN 1L ELSE 0L END), " +
           "AVG(CASE WHEN r.status = 'SUCCESS' THEN r.responseTimeMs ELSE NULL END) " +
           "FROM CheckResult r " +
           "WHERE r.monitor.id IN :monitorIds AND r.checkedAt >= :since " +
           "GROUP BY r.monitor.id")
    List<Object[]> aggregateStatsByMonitorIdsSince(@Param("monitorIds") java.util.Collection<Long> monitorIds, @Param("since") Instant since);
}
