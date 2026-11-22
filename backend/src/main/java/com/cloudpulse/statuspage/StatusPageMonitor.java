package com.cloudpulse.statuspage;

import com.cloudpulse.monitor.Monitor;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "status_page_monitors", uniqueConstraints = {
        @UniqueConstraint(name = "uq_status_page_monitor", columnNames = {"status_page_id", "monitor_id"})
})
public class StatusPageMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_page_id", nullable = false)
    private StatusPage statusPage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StatusPageMonitor() {
    }

    public StatusPageMonitor(StatusPage statusPage, Monitor monitor, int displayOrder) {
        this.statusPage = statusPage;
        this.monitor = monitor;
        this.displayOrder = displayOrder;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public StatusPage getStatusPage() {
        return statusPage;
    }

    public void setStatusPage(StatusPage statusPage) {
        this.statusPage = statusPage;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
