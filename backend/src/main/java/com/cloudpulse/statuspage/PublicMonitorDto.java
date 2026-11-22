package com.cloudpulse.statuspage;

public class PublicMonitorDto {

    private String name;
    private String status;
    private Double uptime24h;
    private Double uptime7d;
    private Double uptime30d;
    private Double averageLatencyMs;

    public PublicMonitorDto() {
    }

    public PublicMonitorDto(String name, String status, Double uptime24h, Double uptime7d, Double uptime30d, Double averageLatencyMs) {
        this.name = name;
        this.status = status;
        this.uptime24h = uptime24h;
        this.uptime7d = uptime7d;
        this.uptime30d = uptime30d;
        this.averageLatencyMs = averageLatencyMs;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Double getUptime24h() {
        return uptime24h;
    }

    public Double getUptime7d() {
        return uptime7d;
    }

    public Double getUptime30d() {
        return uptime30d;
    }

    public Double getAverageLatencyMs() {
        return averageLatencyMs;
    }
}
