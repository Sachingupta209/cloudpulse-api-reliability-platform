package com.cloudpulse.statuspage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusPageMonitorRepository extends JpaRepository<StatusPageMonitor, Long> {

    List<StatusPageMonitor> findByStatusPageIdOrderByDisplayOrderAsc(Long statusPageId);

    @Modifying
    @Query("DELETE FROM StatusPageMonitor spm WHERE spm.statusPage.id = :statusPageId")
    void deleteByStatusPageId(@Param("statusPageId") Long statusPageId);
}
