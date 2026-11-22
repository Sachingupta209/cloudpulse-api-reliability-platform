package com.cloudpulse.monitor;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    @EntityGraph(attributePaths = {"validationRules"})
    List<Monitor> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"validationRules"})
    Optional<Monitor> findByIdAndUserId(Long id, Long userId);

    List<Monitor> findByStatus(MonitorStatus status);

    @Query("SELECT DISTINCT m FROM Monitor m LEFT JOIN FETCH m.validationRules JOIN FETCH m.user WHERE m.id = :id")
    Optional<Monitor> findByIdWithValidationRules(@Param("id") Long id);

    @Query("SELECT DISTINCT m FROM Monitor m LEFT JOIN FETCH m.validationRules JOIN FETCH m.user WHERE m.id = :id AND m.user.id = :userId")
    Optional<Monitor> findByIdAndUserIdWithValidationRules(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT DISTINCT m FROM Monitor m LEFT JOIN FETCH m.validationRules WHERE m.user.id = :userId")
    List<Monitor> findByUserIdWithValidationRules(@Param("userId") Long userId);

    @Query("SELECT DISTINCT m FROM Monitor m LEFT JOIN FETCH m.validationRules WHERE m.status = :status")
    List<Monitor> findByStatusWithValidationRules(@Param("status") MonitorStatus status);

    @Query("SELECT m FROM Monitor m WHERE m.status = 'ACTIVE' AND (m.lastCheckedAt IS NULL OR m.lastCheckedAt <= :dueBefore)")
    List<Monitor> findActiveMonitorsDueForCheck(@Param("dueBefore") Instant dueBefore);

    long countByUserId(Long userId);

    long countByUserIdAndHealthStatus(Long userId, HealthStatus healthStatus);

    long countByUserIdAndStatus(Long userId, MonitorStatus status);
}
